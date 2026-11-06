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

%javaconst(1);

%include "various.i"
%include "enums.swg"

#define PACKAGE_ "com.typedb.driver.jni"
#define PACKAGE_PATH_ "com/typedb/driver/jni"

%typemap(javacode) SWIGTYPE %{
    public $javaclassname released() {
        long cptr = swigCPtr;
        if (swigCMemOwn) {
            swigCPtr = 0;
            swigCMemOwn = false;
        }
        return new $javaclassname(cptr, false);
    }

    public boolean isOwned() {
        return swigCMemOwn;
    }
%}

%typemap(javabase) Error "java.lang.Throwable";
%typemap(javabody) Error %{
    private transient long swigCPtr;
    protected transient boolean swigCMemOwn;

    protected $javaclassname(long cPtr, boolean cMemoryOwn) {
        super((
            new String(typedb_driverJNI.error_code(cPtr, null), java.nio.charset.Charset.forName("UTF-8"))
            + " "
            + new String(typedb_driverJNI.error_message(cPtr, null), java.nio.charset.Charset.forName("UTF-8"))
        ).strip());
        swigCMemOwn = cMemoryOwn;
        swigCPtr = cPtr;
    }

    protected static long getCPtr($javaclassname obj) {
        return (obj == null) ? 0 : obj.swigCPtr;
    } 

    public static class Unchecked extends RuntimeException {
        Unchecked(Error e) {
            super(e.getMessage());
        }
    }
%}

%ignore get_last_error;
%javaexception("Error") {
    $action
    if (check_error()) {
        jenv->ExceptionClear();

        jlong jerror = 0;
        Error* error = get_last_error();
        *(Error**)&jerror = error;
        jclass exceptionClass = jenv->FindClass(PACKAGE_PATH_ "/Error");
        jmethodID ctor = jenv->GetMethodID(exceptionClass, "<init>", "(JZ)V");
        jthrowable throwable = (jthrowable)jenv->NewObject(exceptionClass, ctor, jerror, true);

        jenv->Throw(throwable);
    }
}

%nojavaexception init_logging;

%nojavaexception transaction_options_new;
%nojavaexception transaction_options_get_transaction_timeout_millis;
%nojavaexception transaction_options_set_transaction_timeout_millis;
%nojavaexception transaction_options_has_transaction_timeout_millis;
%nojavaexception transaction_options_get_schema_lock_acquire_timeout_millis;
%nojavaexception transaction_options_set_schema_lock_acquire_timeout_millis;
%nojavaexception transaction_options_has_schema_lock_acquire_timeout_millis;
%nojavaexception query_options_new;
%nojavaexception query_options_get_include_instance_types;
%nojavaexception query_options_set_include_instance_types;
%nojavaexception query_options_has_include_instance_types;
%nojavaexception query_options_get_prefetch_size;
%nojavaexception query_options_set_prefetch_size;
%nojavaexception query_options_has_prefetch_size;
%nojavaexception query_options_set_include_query_structure;
%nojavaexception query_options_get_include_query_structure;
%nojavaexception query_options_has_include_query_structure;

%nojavaexception error_code;
%nojavaexception error_message;

%nojavaexception driver_is_open;

%nojavaexception driver_options_new;
%nojavaexception driver_options_set_tls_config;
%nojavaexception driver_options_get_tls_config;
%nojavaexception driver_options_set_request_timeout_millis;
%nojavaexception driver_options_get_request_timeout_millis;
%nojavaexception driver_options_set_primary_failover_retries;
%nojavaexception driver_options_get_primary_failover_retries;
%nojavaexception driver_tls_config_new_disabled;
%nojavaexception driver_tls_config_new_enabled_with_native_root_ca;
%nojavaexception driver_tls_config_is_enabled;
%nojavaexception driver_tls_config_has_root_ca_path;
%nojavaexception driver_tls_config_get_root_ca_path;

%nojavaexception transaction_is_open;

%nojavaexception user_get_name;
%nojavaexception user_get_password_expiry_seconds;

%nojavaexception server_get_address;
%nojavaexception server_is_primary;
%nojavaexception server_get_id;
%nojavaexception server_get_role;
%nojavaexception server_has_role;
%nojavaexception server_get_term;
%nojavaexception server_has_term;

%nojavaexception database_get_name;

%nojavaexception server_routing_auto;
%nojavaexception server_routing_direct;

%nojavaexception concept_is_entity;
%nojavaexception concept_is_relation;
%nojavaexception concept_is_attribute;
%nojavaexception concept_is_value;
%nojavaexception concept_is_entity_type;
%nojavaexception concept_is_relation_type;
%nojavaexception concept_is_attribute_type;
%nojavaexception concept_is_role_type;
%nojavaexception concept_as_entity;
%nojavaexception concept_as_relation;
%nojavaexception concept_as_attribute;
%nojavaexception concept_as_value;
%nojavaexception concept_as_entity_type;
%nojavaexception concept_as_relation_type;
%nojavaexception concept_as_attribute_type;

%nojavaexception concept_get_label;
%nojavaexception concept_try_get_label;
%nojavaexception concept_try_get_iid;
%nojavaexception concept_try_get_value_type;
%nojavaexception concept_try_get_value;
%nojavaexception concept_to_string;
%nojavaexception concept_equals;

%nojavaexception concept_is_boolean;
%nojavaexception concept_is_date;
%nojavaexception concept_is_datetime;
%nojavaexception concept_is_datetime_tz;
%nojavaexception concept_is_decimal;
%nojavaexception concept_is_double;
%nojavaexception concept_is_duration;
%nojavaexception concept_is_integer;
%nojavaexception concept_is_string;
%nojavaexception concept_is_struct;

// Should call concept_is_X before concept_get_X and throw language-specific exception in impl
%nojavaexception concept_get_boolean;
%nojavaexception concept_get_date_as_seconds;
%nojavaexception concept_get_datetime;
%nojavaexception concept_get_datetime_tz;
%nojavaexception concept_get_decimal;
%nojavaexception concept_get_double;
%nojavaexception concept_get_duration;
%nojavaexception concept_get_integer;
%nojavaexception concept_get_string;
%nojavaexception concept_get_struct;

%nojavaexception concept_new_boolean;
%nojavaexception concept_new_integer;
%nojavaexception concept_new_double;
%nojavaexception concept_new_decimal;
%nojavaexception concept_new_string;
%nojavaexception concept_new_date_from_seconds;
%nojavaexception concept_new_datetime;
%nojavaexception concept_new_datetime_tz_iana;
%nojavaexception concept_new_datetime_tz_offset;
%nojavaexception concept_new_duration;

%nojavaexception entity_get_type;
%nojavaexception relation_get_type;
%nojavaexception attribute_get_type;

%nojavaexception concept_row_equals;
%nojavaexception concept_row_get_column_names;
%nojavaexception concept_row_get_concepts;
%nojavaexception concept_row_get_query_structure;
%nojavaexception concept_row_get_query_type;
%nojavaexception concept_row_involved_conjunctions;
%nojavaexception concept_row_to_string;

%nojavaexception query_answer_get_query_type;
%nojavaexception query_answer_into_rows;
%nojavaexception query_answer_into_documents;
%nojavaexception query_answer_is_ok;
%nojavaexception query_answer_is_concept_row_stream;
%nojavaexception query_answer_is_concept_document_stream;

%nojavaexception given_rows_header_builder_new;
%nojavaexception given_rows_header_builder_push;
%nojavaexception given_rows_header_builder_finish;
%nojavaexception given_rows_builder_new;
%nojavaexception given_rows_builder_start_new_row;


%nojavaexception ServerVersion::distribution;
%nojavaexception ServerVersion::version;

%nojavaexception StringPair::_0;
%nojavaexception StringPair::_1;

%nojavaexception DatetimeAndTimeZone::datetime_in_nanos;
%nojavaexception DatetimeAndTimeZone::zone_name;
%nojavaexception DatetimeAndTimeZone::local_minus_utc_offset;
%nojavaexception DatetimeAndTimeZone::is_fixed_offset;

%nojavaexception DatetimeInNanos::seconds;
%nojavaexception DatetimeInNanos::subsec_nanos;

%nojavaexception StringAndOptValue::string;
%nojavaexception StringAndOptValue::value;

%nojavaexception Decimal::integer;
%nojavaexception Decimal::fractional;

%nojavaexception Duration::months;
%nojavaexception Duration::days;
%nojavaexception Duration::nanos;

%nojavaexception ServerRouting::type_;
%nojavaexception ServerRouting::address;

/* director constructors do not throw */
%nojavaexception TransactionCallbackDirector;

/* field accessors for analyze do not throw */
%nojavaexception analyzed_query_pipeline;
%nojavaexception analyzed_preamble;
%nojavaexception analyzed_given;
%nojavaexception analyzed_fetch;
%nojavaexception comparator_get_name;

%nojavaexception conjunction_id_as_u32;
%nojavaexception conjunction_id_string_repr;
%nojavaexception conjunction_get_constraints;
%nojavaexception conjunction_get_annotated_variables;
%nojavaexception conjunction_get_variable_annotations;

%nojavaexception constraint_span_begin;
%nojavaexception constraint_span_end;
%nojavaexception constraint_variant;
%nojavaexception constraint_isa_get_instance;
%nojavaexception constraint_isa_get_type;
%nojavaexception constraint_has_get_attribute;
%nojavaexception constraint_has_get_owner;
%nojavaexception constraint_links_get_relation;
%nojavaexception constraint_links_get_player;
%nojavaexception constraint_links_get_role;
%nojavaexception constraint_sub_get_subtype;
%nojavaexception constraint_sub_get_supertype;
%nojavaexception constraint_owns_get_owner;
%nojavaexception constraint_owns_get_attribute;
%nojavaexception constraint_relates_get_relation;
%nojavaexception constraint_relates_get_role;
%nojavaexception constraint_plays_get_player;
%nojavaexception constraint_plays_get_role;
%nojavaexception constraint_isa_get_exactness;
%nojavaexception constraint_has_get_exactness;
%nojavaexception constraint_links_get_exactness;
%nojavaexception constraint_sub_get_exactness;
%nojavaexception constraint_owns_get_exactness;
%nojavaexception constraint_relates_get_exactness;
%nojavaexception constraint_plays_get_exactness;
%nojavaexception constraint_function_call_get_name;
%nojavaexception constraint_function_call_get_arguments;
%nojavaexception constraint_function_call_get_assigned;
%nojavaexception constraint_expression_get_text;
%nojavaexception constraint_expression_get_arguments;
%nojavaexception constraint_expression_get_assigned;
%nojavaexception constraint_is_get_lhs;
%nojavaexception constraint_is_get_rhs;
%nojavaexception constraint_iid_get_variable;
%nojavaexception constraint_iid_get_iid;
%nojavaexception constraint_delete_concepts_get_variables;
%nojavaexception constraint_comparison_get_lhs;
%nojavaexception constraint_comparison_get_rhs;
%nojavaexception constraint_comparison_get_comparator;
%nojavaexception constraint_kind_get_kind;
%nojavaexception constraint_kind_get_type;
%nojavaexception constraint_label_get_variable;
%nojavaexception constraint_label_get_label;
%nojavaexception constraint_value_get_attribute_type;
%nojavaexception constraint_value_get_value_type;
%nojavaexception constraint_isset_get_variables;
%nojavaexception constraint_or_get_branches;
%nojavaexception constraint_not_get_conjunction;
%nojavaexception constraint_try_get_conjunction;
%nojavaexception constraint_string_repr;

%nojavaexception constraint_vertex_variant;
%nojavaexception constraint_vertex_as_variable;
%nojavaexception constraint_vertex_as_label;
%nojavaexception constraint_vertex_as_value;
%nojavaexception constraint_vertex_as_named_role;
%nojavaexception named_role_get_name;
%nojavaexception named_role_get_variable;
%nojavaexception named_role_as_u32;
%nojavaexception named_role_string_repr;

%nojavaexception fetch_variant;
%nojavaexception fetch_leaf_annotations;
%nojavaexception fetch_list_element;
%nojavaexception fetch_object_fields;
%nojavaexception fetch_object_get_field;

%nojavaexception function_body;
%nojavaexception function_argument_variables;
%nojavaexception function_return_operation;
%nojavaexception function_argument_annotations;
%nojavaexception function_return_annotations;

%nojavaexception given_variables;
%nojavaexception given_variable_annotations;

%nojavaexception pipeline_get_conjunction;
%nojavaexception pipeline_stages;
%nojavaexception pipeline_stage_variant;
%nojavaexception pipeline_stage_get_block;
%nojavaexception pipeline_stage_delete_get_deleted_variables;
%nojavaexception pipeline_stage_select_get_variables;
%nojavaexception pipeline_stage_require_get_variables;
%nojavaexception pipeline_stage_offset_get_offset;
%nojavaexception pipeline_stage_limit_get_limit;
%nojavaexception pipeline_stage_sort_get_sort_variables;
%nojavaexception pipeline_stage_reduce_get_groupby;
%nojavaexception pipeline_stage_reduce_get_reducer_assignments;
%nojavaexception pipeline_stage_string_repr;
%nojavaexception reduce_assignment_get_assigned;
%nojavaexception reduce_assignment_get_reducer;
%nojavaexception reducer_get_name;
%nojavaexception reducer_get_arguments;
%nojavaexception sort_variable_get_variable;
%nojavaexception sort_variable_get_order;

%nojavaexception return_operation_variant;
%nojavaexception return_operation_stream_variables;
%nojavaexception return_operation_single_selector;
%nojavaexception return_operation_single_variables;
%nojavaexception return_operation_reducers;

%nojavaexception variable_annotations_variant;
%nojavaexception variable_annotations_instance;
%nojavaexception variable_annotations_type;
%nojavaexception variable_annotations_value;

%nojavaexception variable_get_name;
%nojavaexception variable_id_as_u32;
%nojavaexception variable_string_repr;

/* destructors do not throw */
%nojavaexception ~Concept;
%nojavaexception ~ConceptIterator;
%nojavaexception ~ConceptRow;
%nojavaexception ~ConceptRowIterator;
%nojavaexception ~ServerRouting;
%nojavaexception ~DriverOptions;
%nojavaexception ~DriverTlsConfig;
%nojavaexception ~Credentials;
%nojavaexception ~Database;
%nojavaexception ~DatabaseIterator;
%nojavaexception ~DatetimeAndTimeZone;
%nojavaexception ~DatetimeInNanos;
%nojavaexception ~Decimal;
%nojavaexception ~Duration;
%nojavaexception ~Error;
%nojavaexception ~GivenRowsHeaderBuilder;
%nojavaexception ~GivenRowsHeader;
%nojavaexception ~GivenRowsBuilder;
%nojavaexception ~GivenRows;
%nojavaexception ~Server;
%nojavaexception ~ServerIterator;
%nojavaexception ~ServerVersion;
%nojavaexception ~StringIterator;
%nojavaexception ~StringAndOptValue;
%nojavaexception ~StringAndOptValueIterator;
%nojavaexception ~Transaction;
%nojavaexception ~TransactionCallbackDirector;
%nojavaexception ~TransactionOptions;
%nojavaexception ~TypeDBDriver;
%nojavaexception ~QueryAnswer;
%nojavaexception ~QueryOptions;
%nojavaexception ~ValueType;
%nojavaexception ~User;
%nojavaexception ~UserIterator;
%nojavaexception ~Value;

%nojavaexception ~AnalyzedQuery;
%nojavaexception ~Conjunction;
%nojavaexception ~ConjunctionID;
%nojavaexception ~ConstraintWithSpan;
%nojavaexception ~ConstraintVertex;
%nojavaexception ~Fetch;
%nojavaexception ~Given;
%nojavaexception ~Function;
%nojavaexception ~NamedRole;
%nojavaexception ~PipelineStage;
%nojavaexception ~Pipeline;
%nojavaexception ~Reducer;
%nojavaexception ~ReduceAssignment;
%nojavaexception ~ReturnOperation;
%nojavaexception ~ConstraintWithSpanIterator;
%nojavaexception ~ConjunctionIDIterator;
%nojavaexception ~ConstraintVertexIterator;
%nojavaexception ~FunctionIterator;
%nojavaexception ~PipelineStageIterator;
%nojavaexception ~ReducerIterator;
%nojavaexception ~ReduceAssignmentIterator;
%nojavaexception ~SortVariable;
%nojavaexception ~SortVariableIterator;
%nojavaexception ~VariableAnnotations;
%nojavaexception ~VariableAnnotationsIterator;
%nojavaexception ~Variable;
%nojavaexception ~VariableIterator;

%nojavaexception transaction_commit;
%nojavaexception transaction_rollback;

%typemap(javabody) ThrowingSupplier %{
    protected static long getCPtr($javaclassname obj) {
        return (obj == null) ? 0 : obj.swigCPtr;
    } 
%}


%define %promise(Type, function_prefix)
%typemap(javainterfaces) Type ## Promise "java.util.function.Supplier<Type>";
%nojavaexception ~ ## Type ## Promise;
%typemap(javabody) Type ## Promise %{
    private transient long swigCPtr;
    protected transient boolean swigCMemOwn;

    protected $javaclassname(long cPtr, boolean cMemoryOwn) {
        swigCMemOwn = cMemoryOwn;
        swigCPtr = cPtr;
    }

    protected static long getCPtr($javaclassname obj) {
        return (obj == null) ? 0 : obj.swigCPtr;
    } 

    @Override
    public Type get() {
        swigCMemOwn = false;
        try {
            return typedb_driver.function_prefix ## _promise_resolve(this);
        } catch (Error e) {
            throw new Error.Unchecked(e);
        }
    }
%}
%typemap(javadestruct, methodname="delete", methodmodifiers="public synchronized") Type ## Promise %{{
    if (swigCPtr != 0) {
        if (swigCMemOwn) {
            swigCMemOwn = false;
            get();
        }
        swigCPtr = 0;
    }
}%}
%enddef

%promise(AnalyzedQuery, analyzed_query)
%promise(QueryAnswer, query_answer)
%promise(String, string)

/* void promises require special handling */
%typemap(javainterfaces) VoidPromise "java.util.function.Supplier<Void>";
%nojavaexception ~VoidPromise;
%typemap(javacode) VoidPromise %{
    @Override
    public Void get() {
        swigCMemOwn = false;
        try {
            typedb_driver.void_promise_resolve(this);
        } catch (Error e) {
            throw new Error.Unchecked(e);
        }
        return null;
    }
%}
%typemap(javadestruct, methodname="delete", methodmodifiers="public synchronized") VoidPromise %{{
    if (swigCPtr != 0) {
        if (swigCMemOwn) {
            swigCMemOwn = false;
            get();
        }
        swigCPtr = 0;
    }
}%}

/* bool promises require special handling */
%typemap(javainterfaces) BoolPromise "java.util.function.Supplier<Boolean>";
%nojavaexception ~BoolPromise;
%typemap(javacode) BoolPromise %{
    @Override
    public Boolean get() {
        swigCMemOwn = false;
        try {
            return typedb_driver.bool_promise_resolve(this);
        } catch (Error e) {
            throw new Error.Unchecked(e);
        }
    }
%}
%typemap(javadestruct, methodname="delete", methodmodifiers="public synchronized") BoolPromise %{{
    if (swigCPtr != 0) {
        if (swigCMemOwn) {
            swigCMemOwn = false;
            get();
        }
        swigCPtr = 0;
    }
}%}

%define %iterator(Type, function_prefix)
%typemap(javainterfaces) Type ## Iterator "java.util.Iterator<Type>";
%typemap(javacode) Type ## Iterator %{
    private boolean isFinished = false;
    private Type next = null;

    private Type fetchNext() throws Error {
        next = typedb_driver.function_prefix ## _iterator_next(this);
        return next;
    }

    @Override
    public boolean hasNext() {
        if (isFinished) return false;
        if (next == null) {
            try {
                if (fetchNext() == null) {
                    isFinished = true;
                    return false;
                }
            } catch (Error e) {
                throw new Error.Unchecked(e);
            }
        }
        assert(next != null);
        return true;
    }

    @Override
    public Type next() {
        if (next == null && !hasNext()) return null;
        Type ret = next;
        next = null;
        return ret;
    }
%}
%enddef

%iterator(Concept, concept)
%iterator(ConceptRow, concept_row)
%iterator(String, string)
%iterator(StringAndOptValue, string_and_opt_value)
%iterator(User, user)
%iterator(Database, database)
%iterator(Server, server)

%iterator(ConjunctionID, conjunction_id)
%iterator(ConstraintWithSpan, constraint_with_span)
%iterator(ConstraintVertex, constraint_vertex)
%iterator(Function, function)
%iterator(PipelineStage, pipeline_stage)
%iterator(ReduceAssignment, reduce_assignment)
%iterator(Reducer, reducer)
%iterator(SortVariable, sort_variable)
%iterator(VariableAnnotations, variable_annotations)
%iterator(Variable, variable)


%typemap(javabody) QueryAnswer %{
    private transient long swigCPtr;
    protected transient boolean swigCMemOwn;

    protected $javaclassname(long cPtr, boolean cMemoryOwn) {
        swigCMemOwn = cMemoryOwn;
        swigCPtr = cPtr;
    }

    protected static long getCPtr($javaclassname obj) {
        return (obj == null) ? 0 : obj.swigCPtr;
    }

    public ConceptRowIterator intoRows() {
        swigCMemOwn = false;
        return typedb_driver.query_answer_into_rows(this);
    }

    public StringIterator intoDocuments() {
        swigCMemOwn = false;
        return typedb_driver.query_answer_into_documents(this);
    }
%}

%typemap(javadestruct, methodname="delete", methodmodifiers="public synchronized") QueryAnswer %{{
    if (swigCPtr != 0) {
        if (swigCMemOwn) {
            swigCMemOwn = false;
            typedb_driverJNI.delete_QueryAnswer(swigCPtr);
        }
        swigCPtr = 0;
    }
}%}

%define %array(Type)
%typemap(in) Type ** (jint size) {
    int i = 0;
    size = jenv->GetArrayLength($input);
    $1 = (Type **) malloc((size+1)*sizeof(Type *));
    jlong* array = (jlong*)jenv->GetLongArrayElements($input, JNI_FALSE);
    /* make a copy of each ptr */
    for (i = 0; i<size; i++) {
        $1[i] = (Type*)array[i];
    }
    $1[i] = 0;
}

%typemap(freearg) Type ** { free($1); }

%typemap(jni) Type ** "jlongArray"
%typemap(jtype) Type ** "long[]"
%typemap(jstype) Type ** "Type[]"

%typemap(javain) Type ** "java.util.Arrays.stream($javainput).mapToLong(Type::getCPtr).toArray()"
%typemap(javaout) Type ** {
    return $jnicall;
}
%enddef

%array(Concept)

/* Strings: reencode as UTF-8 byte arrays rather than use Java's MUTF-8 */
%typemap(jni) char* "jbyteArray"
%typemap(jtype) char* "byte[]"
%typemap(jstype) char* "String"

%typemap(javain) char* %{
    $javainput.getBytes(java.nio.charset.Charset.forName("UTF-8"))
%}

%typemap(in) char* {
    size_t sz = jenv->GetArrayLength($input);
    $1 = (char*)malloc(sz + 1);
    jbyte* bae = jenv->GetByteArrayElements($input, 0);
    memcpy($1, (const char*)bae, sz);
    $1[sz] = 0;
    jenv->ReleaseByteArrayElements($input, bae, 0);
}

%typemap(freearg) char* { free($1); }

%typemap(out) char* {
    if ($1) {
        size_t sz = strlen($1);
        $result = jenv->NewByteArray(sz);
        jbyte* bae = jenv->GetByteArrayElements($result, 0);
        memcpy((char*)bae, $1, sz);
        jenv->ReleaseByteArrayElements($result, bae, 0);
    } else {
        $result = 0;
    }
}

%typemap(javaout) char* {
    byte[] ba = $jnicall;
    if (ba == null) {
        return null;
    }
    return new String(ba, java.nio.charset.Charset.forName("UTF-8"));
}

/* char** needs special handling */
%typemap(jni) char ** "jobjectArray"
%typemap(jtype) char ** "byte[][]"
%typemap(jstype) char ** "String[]"

%typemap(javain) char ** %{
    java.util.stream.Stream.of($javainput).map(s -> s.getBytes(java.nio.charset.Charset.forName("UTF-8"))).toArray(byte[][]::new)
%}

%typemap(in) char ** (jint size) {
    int i = 0;
    size = jenv->GetArrayLength($input);
    $1 = (char **) malloc((size+1)*sizeof(char *));
    /* make a copy of each string */
    for (i = 0; i<size; i++) {
        jbyteArray j_string = (jbyteArray)jenv->GetObjectArrayElement($input, i);
        size_t sz = jenv->GetArrayLength(j_string);
        const char * c_string = (char*)jenv->GetByteArrayElements(j_string, 0);
        $1[i] = (char*)malloc((sz + 1)*sizeof(char));
        memcpy($1[i], c_string, sz);
        $1[i][sz] = 0;
        jenv->ReleaseByteArrayElements(j_string, (jbyte*)c_string, 0);
        jenv->DeleteLocalRef(j_string);
    }
    $1[i] = 0;
}

%typemap(freearg) char ** {
    int i;
    for (i=0; i<size$argnum-1; i++)
        free($1[i]);
    free($1);
}

