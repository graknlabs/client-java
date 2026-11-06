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

use std::{
    cell::RefCell,
    ffi::{CStr, CString, c_char},
    ptr::null_mut,
    sync::Arc,
};

use tracing::trace;
use typedb_driver::Error;

pub(crate) fn release<T>(t: T) -> *mut T {
    let raw = Box::into_raw(Box::new(t));
    trace!("Releasing ownership of <{}> @ {:?}", std::any::type_name::<T>(), raw);
    raw
}

pub(crate) fn release_optional<T>(t: Option<T>) -> *mut T {
    t.map(release).unwrap_or_else(null_mut)
}

pub(crate) fn release_string(str: String) -> *mut c_char {
    let raw = CString::new(str).unwrap().into_raw();
    trace!("Releasing ownership of <CString> @ {:?}", raw);
    raw
}

pub(crate) fn release_optional_string(str: Option<String>) -> *mut c_char {
    str.map(release_string).unwrap_or_else(null_mut)
}

pub(crate) fn release_arc<T>(t: Arc<T>) -> *const T {
    let raw = Arc::into_raw(t);
    trace!("Releasing ownership of arc <{}> @ {:?}", std::any::type_name::<T>(), raw);
    raw
}

pub(crate) fn take_arc<T>(raw: *const T) -> Arc<T> {
    trace!("Taking ownership of arced <{}> @ {:?}", std::any::type_name::<T>(), raw);
    unsafe { Arc::from_raw(raw) }
}

pub(crate) fn decrement_arc<T>(raw: *const T) {
    trace!("Decrementing arced <{}> @ {:?}", std::any::type_name::<T>(), raw);
    assert!(!raw.is_null());
    drop(take_arc(raw))
}

pub(crate) fn borrow<T>(raw: *const T) -> &'static T {
    trace!("Borrowing <{}> @ {:?}", std::any::type_name::<T>(), raw);
    assert!(!raw.is_null());
    unsafe { &*raw }
}

pub(crate) fn borrow_mut<T>(raw: *mut T) -> &'static mut T {
    trace!("Borrowing (mut) <{}> @ {:?}", std::any::type_name::<T>(), raw);
    assert!(!raw.is_null());
    unsafe { &mut *raw }
}

pub(crate) fn borrow_optional<T>(raw: *const T) -> Option<&'static T> {
    trace!("Borrowing optional (null ok) <{}> @ {:?}", std::any::type_name::<T>(), raw);
    unsafe { raw.as_ref() }
}

pub(crate) fn take_ownership<T>(raw: *mut T) -> T {
    trace!("Taking ownership of <{}> @ {:?}", std::any::type_name::<T>(), raw);
    assert!(!raw.is_null());
    unsafe { *Box::from_raw(raw) }
}

pub(crate) fn free<T>(raw: *mut T) {
    trace!("Freeing <{}> @ {:?}", std::any::type_name::<T>(), raw);
    if !raw.is_null() {
        unsafe { drop(Box::from_raw(raw)) }
    }
}

pub(crate) fn string_view(str: *const c_char) -> &'static str {
    assert!(!str.is_null());
    unsafe {
        let cstr = CStr::from_ptr(str);
        cstr.to_str().unwrap_or_else(|e| panic!("Could not parse {cstr:?}: {e}"))
    }
}

/// Frees a native rust string.
/// _WARNING: Always use this function to free strings returned by the driver. Using the standard C free function will create a dangling reference on the rust side._
#[unsafe(no_mangle)]
pub extern "C" fn string_free(str: *mut c_char) {
    trace!("Freeing <CString> @ {:?}", str);
    if !str.is_null() {
        unsafe { drop(CString::from_raw(str)) }
    }
}

pub(crate) fn array_view<T: 'static>(ts: *const *const T) -> impl Iterator<Item = &'static T> {
    assert!(!ts.is_null());
    unsafe { (0..).map_while(move |i| (*ts.add(i)).as_ref()) }
}

pub(crate) fn string_array_view(strs: *const *const c_char) -> impl Iterator<Item = &'static str> {
    assert!(!strs.is_null());
    unsafe { (0..).map_while(move |i| (*strs.add(i)).as_ref()).map(|p| string_view(p)) }
}
