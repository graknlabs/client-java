/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

use std::{ffi::c_char, ptr::null_mut, str::FromStr};

use chrono::{DateTime, FixedOffset, NaiveDate, NaiveDateTime, NaiveTime, TimeZone as ChronoTimeZone};
use typedb_driver::{
    box_stream,
    concept::{
        Attribute, Concept, Entity, Relation, Value,
        value::{Decimal, Duration, TimeZone},
    },
};

use crate::common::{
    error::try_release,
    iterator::CIterator,
    memory::{
        borrow, borrow_mut, free, release, release_optional, release_optional_string, release_string, string_free,
        string_view, take_ownership,
    },
};

/// A <code>DatetimeInNanos</code> used to represent datetime as a pair of seconds part and
/// a number of nanoseconds since the last seconds boundary.
#[repr(C)]
#[derive(Clone, Copy, Hash, PartialEq, Eq)]
pub struct DatetimeInNanos {
    /// Whole seconds since the Unix epoch.
    seconds: i64,
    /// Nanoseconds elapsed since the last whole-second boundary.
    subsec_nanos: u32,
}

impl DatetimeInNanos {
    pub fn new<TZ: ChronoTimeZone>(datetime: &DateTime<TZ>) -> Self {
        Self { seconds: datetime.timestamp(), subsec_nanos: datetime.timestamp_subsec_nanos() }
    }
}

/// <code>DatetimeAndTimeZone</code> is used to represent time zoned datetime in FFI.
/// Time zone can be represented either as an IANA <code>Tz</code> or as a <code>FixedOffset</code>.
/// Either the zone_name (is_fixed_offset == false) or offset (is_fixed_offset == true) is set.
#[repr(C)]
pub struct DatetimeAndTimeZone {
    /// The datetime expressed as a (seconds, subsec_nanos) pair.
    datetime_in_nanos: DatetimeInNanos,
    /// IANA time zone identifier (e.g. <code>"Europe/London"</code>); set when <code>is_fixed_offset</code> is false.
    zone_name: *mut c_char,
    /// Offset from UTC in seconds; set when <code>is_fixed_offset</code> is true.
    local_minus_utc_offset: i32,
    /// True if the time zone is a fixed offset, false if it is an IANA-named zone.
    is_fixed_offset: bool,
}

impl DatetimeAndTimeZone {
    pub fn new(datetime_tz: &DateTime<TimeZone>) -> Self {
        let (zone_name, offset, is_fixed_offset) = {
            match datetime_tz.timezone() {
                TimeZone::IANA(tz) => (tz.name().to_owned(), 0, false),
                TimeZone::Fixed(offset) => ("".to_owned(), offset.local_minus_utc(), true),
            }
        };
        Self {
            datetime_in_nanos: DatetimeInNanos::new(datetime_tz),
            zone_name: release_string(zone_name),
            local_minus_utc_offset: offset,
            is_fixed_offset,
        }
    }
}

impl Drop for DatetimeAndTimeZone {
    fn drop(&mut self) {
        string_free(self.zone_name);
    }
}

/// Frees the native rust <code>DatetimeAndTimeZone</code> object
#[unsafe(no_mangle)]
pub extern "C" fn datetime_and_time_zone_drop(datetime_tz: *mut DatetimeAndTimeZone) {
    free(datetime_tz);
}

/// Iterator over the <code>StringAndOptValue</code>s representing struct value {field_name: value?} info.
pub struct StringAndOptValueIterator(pub CIterator<StringAndOptValue>);

/// Forwards the <code>StringAndOptValueIterator</code> and returns the next <code>StringAndOptValue</code> if it exists,
/// or null if there are no more elements.
#[unsafe(no_mangle)]
pub extern "C" fn string_and_opt_value_iterator_next(it: *mut StringAndOptValueIterator) -> *mut StringAndOptValue {
    release_optional(borrow_mut(it).0.0.next())
}

/// Frees the native rust <code>StringAndOptValueIterator</code> object
#[unsafe(no_mangle)]
pub extern "C" fn string_and_opt_value_iterator_drop(it: *mut StringAndOptValueIterator) {
    free(it);
}

/// A <code>StringAndOptValue</code> used to represent the pair of variables involved in an ownership.
/// <code>_0</code> and <code>_1</code> are the owner and attribute variables respectively.
#[repr(C)]
pub struct StringAndOptValue {
    /// The string component of the pair (e.g. a struct field name).
    string: *mut c_char,
    /// Optional <code>Concept</code> value associated with the string; may be null.
    value: *mut Concept,
}

impl From<(String, Option<Value>)> for StringAndOptValue {
    fn from((field_name, value): (String, Option<Value>)) -> Self {
        Self { string: release_string(field_name), value: release_optional(value.map(Concept::Value)) }
    }
}

impl Drop for StringAndOptValue {
    fn drop(&mut self) {
        string_free(self.string);
        concept_drop(self.value);
    }
}

/// Frees the native rust <code>StringAndOptValue</code> object
#[unsafe(no_mangle)]
pub extern "C" fn string_and_opt_value_drop(string_and_opt_value: *mut StringAndOptValue) {
    free(string_and_opt_value);
}

/// Retrieves the unique id (IID) of this <code>Concept</code>.
/// If this is an Entity or Relation Instance, returns the IID of the instance.
/// Otherwise, returns null.
#[unsafe(no_mangle)]
pub extern "C" fn concept_try_get_iid(instance: *mut Concept) -> *mut c_char {
    release_optional_string(borrow(instance).try_get_iid().map(|iid| iid.to_string()))
}

/// Retrieves the label of this <code>Concept</code>.
/// If this is an <code>Instance</code>, returns the label of the type of this instance ("unknown" if type fetching is disabled).
/// If this is a <code>Value</code>, returns the label of the value type of the value.
/// If this is a <code>Type</code>, returns the label of the type.
#[unsafe(no_mangle)]
pub extern "C" fn concept_get_label(concept: *const Concept) -> *mut c_char {
    release_string(borrow(concept).get_label().to_owned())
}

/// Retrieves the optional label of this <code>Concept</code>.
/// If this is an <code>Instance</code>, returns the label of the type of this instance (None if type fetching is disabled).
/// If this is a <code>Value</code>, returns the label of the value type of the value.
/// If this is a <code>Type</code>, returns the label of the type.
#[unsafe(no_mangle)]
pub extern "C" fn concept_try_get_label(concept: *const Concept) -> *mut c_char {
    release_optional_string(borrow(concept).try_get_label().map(|str| str.to_owned()))
}

/// Retrieves the value type of this <code>Concept</code>, if it exists.
/// If this is an <code>Attribute</code> instance, returns the value type of this instance.
/// If this is a <code>Value</code>, returns its value type.
/// If this is an <code>AttributeType</code>, returns the value type that the schema permits for the attribute type, if one is defined.
/// Otherwise, returns null.
#[unsafe(no_mangle)]
pub extern "C" fn concept_try_get_value_type(concept: *const Concept) -> *mut c_char {
    release_optional_string(borrow(concept).try_get_value_label().map(|str| str.to_owned()))
}

/// Retrieves the value of this <code>Concept</code>, if it exists.
/// If this is an <code>Attribute</code> instance, returns the value of this instance.
/// If this a <code>Value</code>, returns the value.
/// Otherwise, returns null.
#[unsafe(no_mangle)]
pub extern "C" fn concept_try_get_value(concept: *const Concept) -> *mut Concept {
    release_optional(borrow(concept).try_get_value().map(|value| Concept::Value(value.clone())))
}

/// Returns <code>true</code> if the value which this <code>Concept</code> holds is of type <code>boolean</code>.
/// Otherwise, returns <code>false</code>.
#[unsafe(no_mangle)]
pub extern "C" fn concept_is_boolean(concept: *const Concept) -> bool {
    borrow(concept).is_boolean()
}

/// Returns <code>true</code> if the value which this <code>Concept</code> holds is of type <code>integer</code>.
/// Otherwise, returns <code>false</code>.
#[unsafe(no_mangle)]
pub extern "C" fn concept_is_integer(concept: *const Concept) -> bool {
    borrow(concept).is_integer()
}

/// Returns <code>true</code> if the value which this <code>Concept</code> holds is of type <code>double</code>.
/// Otherwise, returns <code>false</code>.
#[unsafe(no_mangle)]
pub extern "C" fn concept_is_double(concept: *const Concept) -> bool {
    borrow(concept).is_double()
}

/// Returns <code>true</code> if the value which this <code>Concept</code> holds is of type <code>decimal</code>.
/// Otherwise, returns <code>false</code>.
#[unsafe(no_mangle)]
pub extern "C" fn concept_is_decimal(concept: *const Concept) -> bool {
    borrow(concept).is_decimal()
}

/// Returns <code>true</code> if the value which this <code>Concept</code> holds is of type <code>string</code>.
/// Otherwise, returns <code>false</code>.
#[unsafe(no_mangle)]
pub extern "C" fn concept_is_string(concept: *const Concept) -> bool {
    borrow(concept).is_string()
}

/// Returns <code>true</code> if the value which this <code>Concept</code> holds is of type <code>date</code>.
/// Otherwise, returns <code>false</code>.
#[unsafe(no_mangle)]
pub extern "C" fn concept_is_date(concept: *const Concept) -> bool {
    borrow(concept).is_date()
}

/// Returns <code>true</code> if the value which this <code>Concept</code> holds is of type <code>datetime</code>.
/// Otherwise, returns <code>false</code>.
#[unsafe(no_mangle)]
pub extern "C" fn concept_is_datetime(concept: *const Concept) -> bool {
    borrow(concept).is_datetime()
}

/// Returns <code>true</code> if the value which this <code>Concept</code> holds is of type <code>datetime-tz</code>.
/// Otherwise, returns <code>false</code>.
#[unsafe(no_mangle)]
pub extern "C" fn concept_is_datetime_tz(concept: *const Concept) -> bool {
    borrow(concept).is_datetime_tz()
}

/// Returns <code>true</code> if the value which this <code>Concept</code> holds is of type <code>duration</code>.
/// Otherwise, returns <code>false</code>.
#[unsafe(no_mangle)]
pub extern "C" fn concept_is_duration(concept: *const Concept) -> bool {
    borrow(concept).is_duration()
}

/// Returns <code>true</code> if the value which this <code>Concept</code> holds is of type <code>struct</code>.
/// Otherwise, returns <code>false</code>.
#[unsafe(no_mangle)]
pub extern "C" fn concept_is_struct(concept: *const Concept) -> bool {
    borrow(concept).is_struct()
}

/// Returns a <code>boolean</code> value of this value concept.
/// If the value has another type, the error is set.
#[unsafe(no_mangle)]
pub extern "C" fn concept_get_boolean(concept: *const Concept) -> bool {
    match borrow(concept).try_get_boolean() {
        Some(value) => value,
        None => unreachable!("Attempting to unwrap a non-boolean {:?} as boolean", borrow(concept)),
    }
}

/// Returns the <code>integer</code> value of this value concept.
/// If the value has another type, the error is set.
#[unsafe(no_mangle)]
pub extern "C" fn concept_get_integer(concept: *const Concept) -> i64 {
    match borrow(concept).try_get_integer() {
        Some(value) => value,
        None => unreachable!("Attempting to unwrap a non-integer {:?} as integer", borrow(concept)),
    }
}

/// Returns the <code>double</code> value of this value concept.
/// If the value has another type, the error is set.
#[unsafe(no_mangle)]
pub extern "C" fn concept_get_double(concept: *const Concept) -> f64 {
    match borrow(concept).try_get_double() {
        Some(value) => value,
        None => unreachable!("Attempting to unwrap a non-double {:?} as double", borrow(concept)),
    }
}

/// Returns the <code>decimal</code> value of this value concept.
/// If the value has another type, the error is set.
#[unsafe(no_mangle)]
pub extern "C" fn concept_get_decimal(concept: *const Concept) -> Decimal {
    match borrow(concept).try_get_decimal() {
        Some(value) => value,
        None => unreachable!("Attempting to unwrap a non-decimal {:?} as decimal", borrow(concept)),
    }
}

/// Returns the <code>string</code> value of this value concept.
/// If the value has another type, the error is set.
#[unsafe(no_mangle)]
pub extern "C" fn concept_get_string(concept: *const Concept) -> *mut c_char {
    match borrow(concept).try_get_string() {
        Some(value) => release_string(value.to_owned()),
        None => unreachable!("Attempting to unwrap a non-string {:?} as string", borrow(concept)),
    }
}

/// Returns the value of this date value concept as seconds since the start of the UNIX epoch.
/// If the value has another type, the error is set.
#[unsafe(no_mangle)]
pub extern "C" fn concept_get_date_as_seconds(concept: *const Concept) -> i64 {
    match borrow(concept).try_get_date() {
        Some(value) => value.and_time(NaiveTime::MIN).and_utc().timestamp(),
        None => unreachable!("Attempting to unwrap a non-date {:?} as date", borrow(concept)),
    }
}

/// Returns the value of this datetime value concept as seconds and nanoseconds parts since the start of the UNIX epoch.
/// If the value has another type, the error is set.
#[unsafe(no_mangle)]
pub extern "C" fn concept_get_datetime(concept: *const Concept) -> DatetimeInNanos {
    match borrow(concept).try_get_datetime() {
        Some(value) => DatetimeInNanos::new(&value.and_utc()),
        None => unreachable!("Attempting to unwrap a non-datetime {:?} as datetime", borrow(concept)),
    }
}

/// Returns the value of this datetime-tz value concept as seconds and nanoseconds parts since the start of the UNIX epoch and timezone information.
/// If the value has another type, the error is set.
#[unsafe(no_mangle)]
pub extern "C" fn concept_get_datetime_tz(concept: *const Concept) -> *mut DatetimeAndTimeZone {
    match borrow(concept).try_get_datetime_tz() {
        Some(value) => release(DatetimeAndTimeZone::new(&value)),
        None => unreachable!("Attempting to unwrap a non-datetime-tz {:?} as datetime-tz", borrow(concept)),
    }
}

/// Returns the value of this duration value.
/// If the value has another type, the error is set.
#[unsafe(no_mangle)]
pub extern "C" fn concept_get_duration(concept: *const Concept) -> Duration {
    match borrow(concept).try_get_duration() {
        Some(value) => value,
        None => unreachable!("Attempting to unwrap a non-duration {:?} as duration", borrow(concept)),
    }
}

/// Returns the value of this struct value concept represented as an iterator of {field_name: value?} pairs.
/// If the value has another type, the error is set.
#[unsafe(no_mangle)]
pub extern "C" fn concept_get_struct(concept: *const Concept) -> *mut StringAndOptValueIterator {
    match borrow(concept).try_get_struct() {
        Some(value) => release(StringAndOptValueIterator(CIterator(box_stream(
            value.fields().clone().into_iter().map(|pair| pair.into()),
        )))),
        None => unreachable!("Attempting to unwrap a non-struct {:?} as struct", borrow(concept)),
    }
}

/// Checks whether the provided <code>Concept</code> objects are equal
#[unsafe(no_mangle)]
pub extern "C" fn concept_equals(lhs: *const Concept, rhs: *const Concept) -> bool {
    borrow(lhs) == borrow(rhs)
}

/// Frees the native rust <code>Concept</code> object
#[unsafe(no_mangle)]
pub extern "C" fn concept_drop(concept: *mut Concept) {
    free(concept);
}

/// Checks if this <code>Concept</code> is an <code>EntityType</code>.
#[unsafe(no_mangle)]
pub extern "C" fn concept_is_entity_type(concept: *const Concept) -> bool {
    borrow(concept).is_entity_type()
}

/// Checks if this <code>Concept</code> is a <code>RelationType</code>.
#[unsafe(no_mangle)]
pub extern "C" fn concept_is_relation_type(concept: *const Concept) -> bool {
    borrow(concept).is_relation_type()
}

/// Checks if this <code>Concept</code> is an <code>AttributeType</code>.
#[unsafe(no_mangle)]
pub extern "C" fn concept_is_attribute_type(concept: *const Concept) -> bool {
    borrow(concept).is_attribute_type()
}

/// Checks if this <code>Concept</code> is a <code>RoleType</code>.
#[unsafe(no_mangle)]
pub extern "C" fn concept_is_role_type(concept: *const Concept) -> bool {
    borrow(concept).is_role_type()
}

/// Checks if this <code>Concept</code> is an <code>Entity</code>.
#[unsafe(no_mangle)]
pub extern "C" fn concept_is_entity(concept: *const Concept) -> bool {
    borrow(concept).is_entity()
}

/// Checks if this <code>Concept</code> is a <code>Relation</code>.
#[unsafe(no_mangle)]
pub extern "C" fn concept_is_relation(concept: *const Concept) -> bool {
    borrow(concept).is_relation()
}

/// Checks if this <code>Concept</code> is an <code>Attribute</code>.
#[unsafe(no_mangle)]
pub extern "C" fn concept_is_attribute(concept: *const Concept) -> bool {
    borrow(concept).is_attribute()
}

/// Checks if this <code>Concept</code> is a <code>Value</code>.
#[unsafe(no_mangle)]
pub extern "C" fn concept_is_value(concept: *const Concept) -> bool {
    borrow(concept).is_value()
}

/// A string representation of this <code>Concept</code> object.
#[unsafe(no_mangle)]
pub extern "C" fn concept_to_string(concept: *const Concept) -> *mut c_char {
    release_string(format!("{:?}", borrow(concept)))
}

/// Creates a new <code>Concept</code> object wrapping the specified <code>bool</code> value
#[unsafe(no_mangle)]
pub extern "C" fn concept_new_boolean(value: bool) -> *mut Concept {
    release(Concept::Value(Value::Boolean(value)))
}

/// Creates a new <code>Concept</code> object wrapping the specified <code>i64</code> value
#[unsafe(no_mangle)]
pub extern "C" fn concept_new_integer(value: i64) -> *mut Concept {
    release(Concept::Value(Value::Integer(value)))
}

/// Creates a new <code>Concept</code> object wrapping the specified <code>String</code> value
#[unsafe(no_mangle)]
pub extern "C" fn concept_new_string(value: *const c_char) -> *mut Concept {
    release(Concept::Value(Value::String(string_view(value).to_owned())))
}

/// Creates a new <code>Concept</code> object wrapping the specified <code>f64</code> value
#[unsafe(no_mangle)]
pub extern "C" fn concept_new_double(value: f64) -> *mut Concept {
    release(Concept::Value(Value::Double(value)))
}

/// Creates a new <code>Concept</code> object wrapping the specified <code>Decimal</code> value,
/// provided as a string (without the dec suffix).
#[unsafe(no_mangle)]
pub extern "C" fn concept_new_decimal_from_string(str: *const c_char) -> *mut Concept {
    let result = Decimal::from_str(string_view(str)).map(|value| Concept::Value(Value::Decimal(value)));
    try_release(result)
}

/// Creates a new <code>Concept</code> object wrapping the specified <code>Decimal</code> value,
/// provided as its integer and fractional parts. The fractional part is in units of 10^-19.
#[unsafe(no_mangle)]
pub extern "C" fn concept_new_decimal(integer: i64, fractional: u64) -> *mut Concept {
    release(Concept::Value(Value::Decimal(Decimal::from_parts(integer, fractional))))
}

/// Creates a new <code>Concept</code> object wrapping a specified <code>NaiveDate</code> value.
/// The value must be seconds since the start of the UNIX epoch.
#[unsafe(no_mangle)]
pub extern "C" fn concept_new_date_from_seconds(seconds_since_epoch: i64) -> *mut Concept {
    let naive_date = DateTime::from_timestamp(seconds_since_epoch, 0).unwrap().date_naive();
    release(Concept::Value(Value::Date(naive_date)))
}

/// Creates a new <code>Concept</code> object wrapping the specified naive datetime value,
/// provided as seconds and nanoseconds since the Unix epoch.
#[unsafe(no_mangle)]
pub extern "C" fn concept_new_datetime(seconds: i64, subsec_nanos: u32) -> *mut Concept {
    let naive_datetime = DateTime::from_timestamp(seconds, subsec_nanos).unwrap().naive_utc();
    release(Concept::Value(Value::Datetime(naive_datetime)))
}

/// Creates a new <code>Concept</code> object wrapping the specified timezone-aware datetime value
/// with an IANA timezone name, provided as seconds and nanoseconds since the Unix epoch.
#[unsafe(no_mangle)]
pub extern "C" fn concept_new_datetime_tz_iana(
    seconds: i64,
    subsec_nanos: u32,
    zone_name: *const c_char,
) -> *mut Concept {
    let naive_datetime = DateTime::from_timestamp(seconds, subsec_nanos).unwrap().naive_utc();
    let tz = chrono_tz::Tz::from_str_insensitive(string_view(zone_name)).expect("Invalid timezone");
    release(Concept::Value(Value::DatetimeTZ(TimeZone::IANA(tz).from_utc_datetime(&naive_datetime))))
}

/// Creates a new <code>Concept</code> object wrapping the specified timezone-aware datetime value
/// with a fixed UTC offset in seconds, provided as seconds and nanoseconds since the Unix epoch.
#[unsafe(no_mangle)]
pub extern "C" fn concept_new_datetime_tz_offset(seconds: i64, subsec_nanos: u32, offset_seconds: i32) -> *mut Concept {
    let naive_datetime = DateTime::from_timestamp(seconds, subsec_nanos).unwrap().naive_utc();
    let offset = FixedOffset::east_opt(offset_seconds).expect("Invalid offset");
    release(Concept::Value(Value::DatetimeTZ(TimeZone::Fixed(offset).from_utc_datetime(&naive_datetime))))
}

/// Creates a new <code>Concept</code> object wrapping the specified <code>Duration</code> value,
/// provided as its months, days, and nanoseconds components.
#[unsafe(no_mangle)]
pub extern "C" fn concept_new_duration(months: u32, days: u32, nanos: u64) -> *mut Concept {
    release(Concept::Value(Value::Duration(Duration::new(months, days, nanos))))
}

pub(super) fn borrow_as_entity(concept: *const Concept) -> &'static Entity {
    match borrow(concept) {
        Concept::Entity(entity) => entity,
        _ => unreachable!(),
    }
}

pub(super) fn borrow_as_relation(concept: *const Concept) -> &'static Relation {
    match borrow(concept) {
        Concept::Relation(relation) => relation,
        _ => unreachable!(),
    }
}

pub(super) fn borrow_as_attribute(concept: *const Concept) -> &'static Attribute {
    match borrow(concept) {
        Concept::Attribute(attribute) => attribute,
        _ => unreachable!(),
    }
}
