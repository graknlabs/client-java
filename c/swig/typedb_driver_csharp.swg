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

%csconst(1);

%typemap(cscode) SWIGTYPE %{
    public $csclassname Released()
    {
        var cPtr = swigCPtr.Handle;
        if (swigCMemOwn)
        {
            swigCPtr = new global::System.Runtime.InteropServices.HandleRef(null, global::System.IntPtr.Zero);
            swigCMemOwn = false;
        }

        return new $csclassname(cPtr, false);
    }

    public bool IsOwned()
    {
        return swigCMemOwn;
    }
%}


%typemap(csbase) Error "System.Exception";
%typemap(csbody) Error %{
    private global::System.Runtime.InteropServices.HandleRef swigCPtr;
    protected bool swigCMemOwn;

    internal Error(global::System.IntPtr cPtr, bool cMemoryOwn)
        : base((typedb_driverPINVOKE.error_code(new global::System.Runtime.InteropServices.HandleRef(null, cPtr)) + " "
            + typedb_driverPINVOKE.error_message(new global::System.Runtime.InteropServices.HandleRef(null, cPtr))).Trim())
    {
        swigCMemOwn = cMemoryOwn;
        swigCPtr = new global::System.Runtime.InteropServices.HandleRef(this, cPtr);
    }

    internal static global::System.Runtime.InteropServices.HandleRef getCPtr(Error obj)
    {
        return (obj == null)
            ? new global::System.Runtime.InteropServices.HandleRef(null, global::System.IntPtr.Zero)
            : obj.swigCPtr;
    }

    internal static global::System.Runtime.InteropServices.HandleRef swigRelease(Error obj)
    {
      if (obj == null)
      {
          return new global::System.Runtime.InteropServices.HandleRef(null, global::System.IntPtr.Zero);
      }

      if (!obj.swigCMemOwn)
      {
        throw new global::System.ApplicationException("Cannot release ownership as memory is not owned");
      }

      global::System.Runtime.InteropServices.HandleRef ptr = obj.swigCPtr;
      obj.swigCMemOwn = false;
      obj.Dispose();

      return ptr;
    }
%}

%insert(runtime) %{
    // Code to handle throwing of C# CustomApplicationException from C/C++ code.
    // The equivalent delegate to the callback, CSharpExceptionCallback_t, is NativeExceptionDelegate
    // and the equivalent nativeExceptionCallback instance is nativeDelegate
    typedef void (SWIGSTDCALL* CSharpExceptionCallback_t)(int*);
    CSharpExceptionCallback_t nativeExceptionCallback = NULL;

    extern "C" SWIGEXPORT
    void SWIGSTDCALL NativeExceptionRegisterCallback(CSharpExceptionCallback_t nativeCallback)
    {
        nativeExceptionCallback = nativeCallback;
    }

    // Note that SWIG detects any method calls named starting with
    // SWIG_CSharpSetPendingException and warns if its exception is not used where needed.
    static void SWIG_CSharpSetPendingExceptionNative(int* error)
    {
        nativeExceptionCallback(error);
    }
%}

%pragma(csharp) imclasscode=%{
    class NativeExceptionHelper
    {
        // C# delegate for the C/C++ nativeExceptionCallback
        public delegate void NativeExceptionDelegate(global::System.IntPtr error);
        static NativeExceptionDelegate s_nativeDelegate =
            new NativeExceptionDelegate(SetPendingNativeException);

        [global::System.Runtime.InteropServices.DllImport("$dllimport", EntryPoint="NativeExceptionRegisterCallback")]
        public static extern void NativeExceptionRegisterCallback(NativeExceptionDelegate nativeCallback);

        static void SetPendingNativeException(global::System.IntPtr error)
        {
            SWIGPendingException.Set(new Error(error, true));
        }

        static NativeExceptionHelper()
        {
            NativeExceptionRegisterCallback(s_nativeDelegate);
        }
    }

    static NativeExceptionHelper exceptionHelper = new NativeExceptionHelper();
%}

%exception %{
    $action

    if (check_error())
    {
        SWIG_CSharpSetPendingExceptionNative((int*)get_last_error());
    }
%}

%noexception init_logging;

/* Error functions do not throw */
%noexception error_code;
%noexception error_message;

/* Driver state check does not throw */
%noexception driver_is_open;

/* Transaction state check does not throw */
%noexception transaction_is_open;

/* User property getters do not throw */
%noexception user_get_name;
%noexception user_get_password_expiry_seconds;

/* Driver options do not throw */
%noexception driver_options_new;
%noexception driver_options_set_tls_config;
%noexception driver_options_get_tls_config;
%noexception driver_options_set_request_timeout_millis;
%noexception driver_options_get_request_timeout_millis;
%noexception driver_options_set_primary_failover_retries;
%noexception driver_options_get_primary_failover_retries;

/* TLS config do not throw */
%noexception driver_tls_config_new_disabled;
%noexception driver_tls_config_new_enabled_with_native_root_ca;
%noexception driver_tls_config_is_enabled;
%noexception driver_tls_config_has_root_ca_path;
%noexception driver_tls_config_get_root_ca_path;

/* Server property getters do not throw */
%noexception server_get_address;
%noexception server_is_primary;
%noexception server_get_id;
%noexception server_get_role;
%noexception server_has_role;
%noexception server_get_term;
%noexception server_has_term;

/* Server routing do not throw */
%noexception server_routing_auto;
%noexception server_routing_direct;

/* Database property getters do not throw */
%noexception database_get_name;

/* Query options do not throw */
%noexception query_options_new;
%noexception query_options_get_include_instance_types;
%noexception query_options_get_include_query_structure;
%noexception query_options_get_prefetch_size;
%noexception query_options_has_include_instance_types;
%noexception query_options_has_include_query_structure;
%noexception query_options_has_prefetch_size;
%noexception query_options_set_include_instance_types;
%noexception query_options_set_include_query_structure;
%noexception query_options_set_prefetch_size;

/* Transaction options do not throw */
%noexception transaction_options_new;
%noexception transaction_options_get_transaction_timeout_millis;
%noexception transaction_options_get_schema_lock_acquire_timeout_millis;
%noexception transaction_options_has_transaction_timeout_millis;
%noexception transaction_options_has_schema_lock_acquire_timeout_millis;
%noexception transaction_options_set_transaction_timeout_millis;
%noexception transaction_options_set_schema_lock_acquire_timeout_millis;

/* Query answer getters do not throw */
%noexception query_answer_get_query_type;
%noexception query_answer_is_ok;
%noexception query_answer_is_concept_row_stream;
%noexception query_answer_is_concept_document_stream;
%noexception query_answer_into_rows;
%noexception query_answer_into_documents;

/* Concept row getters do not throw */
%noexception concept_row_get_query_type;
%noexception concept_row_get_query_structure;
%noexception concept_row_get_column_names;
%noexception concept_row_get_concepts;
%noexception concept_row_involved_conjunctions;
%noexception concept_row_equals;
%noexception concept_row_to_string;

/* Concept type checks do not throw */
%noexception concept_is_entity;
%noexception concept_is_relation;
%noexception concept_is_attribute;
%noexception concept_is_value;
%noexception concept_is_entity_type;
%noexception concept_is_relation_type;
%noexception concept_is_attribute_type;
%noexception concept_is_role_type;

/* Concept casts do not throw */
%noexception concept_as_entity;
%noexception concept_as_relation;
%noexception concept_as_attribute;
%noexception concept_as_value;
%noexception concept_as_entity_type;
%noexception concept_as_relation_type;
%noexception concept_as_attribute_type;

/* Concept value type checks do not throw */
%noexception concept_is_boolean;
%noexception concept_is_integer;
%noexception concept_is_double;
%noexception concept_is_decimal;
%noexception concept_is_string;
%noexception concept_is_date;
%noexception concept_is_datetime;
%noexception concept_is_datetime_tz;
%noexception concept_is_duration;
%noexception concept_is_struct;

/* Concept value getters do not throw - call concept_is_X before concept_get_X */
%noexception concept_get_boolean;
%noexception concept_get_date_as_seconds;
%noexception concept_get_datetime;
%noexception concept_get_datetime_tz;
%noexception concept_get_decimal;
%noexception concept_get_double;
%noexception concept_get_duration;
%noexception concept_get_integer;
%noexception concept_get_string;
%noexception concept_get_struct;

/* Concept metadata accessors do not throw */
%noexception concept_get_label;
%noexception concept_try_get_label;
%noexception concept_try_get_iid;
%noexception concept_try_get_value_type;
%noexception concept_try_get_value;
%noexception concept_to_string;
%noexception concept_equals;

/* Instance type getters do not throw */
%noexception entity_get_type;
%noexception relation_get_type;
%noexception attribute_get_type;

/* ServerVersion field accessors do not throw */
%noexception ServerVersion::distribution;
%noexception ServerVersion::version;

/* ServerRouting field accessors do not throw */
%noexception ServerRouting::type_;
%noexception ServerRouting::address;

/* Struct field accessors do not throw */
%noexception StringPair::_0;
%noexception StringPair::_1;
%noexception DatetimeAndTimeZone::datetime_in_nanos;
%noexception DatetimeAndTimeZone::zone_name;
%noexception DatetimeAndTimeZone::local_minus_utc_offset;
%noexception DatetimeAndTimeZone::is_fixed_offset;
%noexception DatetimeInNanos::seconds;
%noexception DatetimeInNanos::subsec_nanos;
%noexception StringAndOptValue::string;
%noexception StringAndOptValue::value;
%noexception Decimal::integer;
%noexception Decimal::fractional;
%noexception Duration::months;
%noexception Duration::days;
%noexception Duration::nanos;

/* Analyze field accessors do not throw */
%noexception analyzed_query_pipeline;
%noexception analyzed_preamble;
%noexception analyzed_fetch;
%noexception analyzed_given;
%noexception comparator_get_name;

%noexception conjunction_id_as_u32;
%noexception conjunction_id_string_repr;
%noexception conjunction_get_constraints;
%noexception conjunction_get_annotated_variables;
%noexception conjunction_get_variable_annotations;

%noexception constraint_span_begin;
%noexception constraint_span_end;
%noexception constraint_variant;
%noexception constraint_isa_get_instance;
%noexception constraint_isa_get_type;
%noexception constraint_has_get_attribute;
%noexception constraint_has_get_owner;
%noexception constraint_links_get_relation;
%noexception constraint_links_get_player;
%noexception constraint_links_get_role;
%noexception constraint_sub_get_subtype;
%noexception constraint_sub_get_supertype;
%noexception constraint_owns_get_owner;
%noexception constraint_owns_get_attribute;
%noexception constraint_relates_get_relation;
%noexception constraint_relates_get_role;
%noexception constraint_plays_get_player;
%noexception constraint_plays_get_role;
%noexception constraint_isa_get_exactness;
%noexception constraint_has_get_exactness;
%noexception constraint_links_get_exactness;
%noexception constraint_sub_get_exactness;
%noexception constraint_owns_get_exactness;
%noexception constraint_relates_get_exactness;
%noexception constraint_plays_get_exactness;
%noexception constraint_function_call_get_name;
%noexception constraint_function_call_get_arguments;
%noexception constraint_function_call_get_assigned;
%noexception constraint_expression_get_text;
%noexception constraint_expression_get_arguments;
%noexception constraint_expression_get_assigned;
%noexception constraint_is_get_lhs;
%noexception constraint_is_get_rhs;
%noexception constraint_iid_get_variable;
%noexception constraint_iid_get_iid;
%noexception constraint_delete_concepts_get_variables;
%noexception constraint_comparison_get_lhs;
%noexception constraint_comparison_get_rhs;
%noexception constraint_comparison_get_comparator;
%noexception constraint_kind_get_kind;
%noexception constraint_kind_get_type;
%noexception constraint_label_get_variable;
%noexception constraint_label_get_label;
%noexception constraint_value_get_attribute_type;
%noexception constraint_value_get_value_type;
%noexception constraint_isset_get_variables;
%noexception constraint_or_get_branches;
%noexception constraint_not_get_conjunction;
%noexception constraint_try_get_conjunction;
%noexception constraint_string_repr;

%noexception constraint_vertex_variant;
%noexception constraint_vertex_as_variable;
%noexception constraint_vertex_as_label;
%noexception constraint_vertex_as_value;
%noexception constraint_vertex_as_named_role;
%noexception named_role_get_name;
%noexception named_role_get_variable;
%noexception named_role_as_u32;
%noexception named_role_string_repr;

%noexception fetch_variant;
%noexception fetch_leaf_annotations;
%noexception fetch_list_element;
%noexception fetch_object_fields;
%noexception fetch_object_get_field;

%noexception function_body;
%noexception function_argument_variables;
%noexception function_return_operation;
%noexception function_argument_annotations;
%noexception function_return_annotations;

%noexception given_variables;
%noexception given_variable_annotations;

%noexception pipeline_get_conjunction;
%noexception pipeline_stages;
%noexception pipeline_stage_variant;
%noexception pipeline_stage_get_block;
%noexception pipeline_stage_delete_get_deleted_variables;
%noexception pipeline_stage_select_get_variables;
%noexception pipeline_stage_require_get_variables;
%noexception pipeline_stage_offset_get_offset;
%noexception pipeline_stage_limit_get_limit;
%noexception pipeline_stage_sort_get_sort_variables;
%noexception pipeline_stage_reduce_get_groupby;
%noexception pipeline_stage_reduce_get_reducer_assignments;
%noexception pipeline_stage_string_repr;
%noexception reduce_assignment_get_assigned;
%noexception reduce_assignment_get_reducer;
%noexception reducer_get_name;
%noexception reducer_get_arguments;
%noexception sort_variable_get_variable;
%noexception sort_variable_get_order;

%noexception return_operation_variant;
%noexception return_operation_stream_variables;
%noexception return_operation_single_selector;
%noexception return_operation_single_variables;
%noexception return_operation_reducers;

%noexception variable_annotations_variant;
%noexception variable_annotations_instance;
%noexception variable_annotations_type;
%noexception variable_annotations_value;

%noexception variable_get_name;
%noexception variable_id_as_u32;
%noexception variable_string_repr;

/* Director constructor does not throw */
%noexception TransactionCallbackDirector;

/* Destructors do not throw */
%noexception ~TypeDBDriver;
%noexception ~Credentials;
%noexception ~DriverOptions;
%noexception ~DriverTlsConfig;
%noexception ~Database;
%noexception ~DatabaseIterator;
%noexception ~Error;
%noexception ~QueryAnswer;
%noexception ~QueryOptions;
%noexception ~TransactionOptions;
%noexception ~ConceptRowIterator;
%noexception ~ConceptRow;
%noexception ~Concept;
%noexception ~ConceptIterator;
%noexception ~StringIterator;
%noexception ~StringAndOptValueIterator;
%noexception ~Transaction;
%noexception ~TransactionCallbackDirector;
%noexception ~User;
%noexception ~UserIterator;
%noexception ~Server;
%noexception ~ServerIterator;
%noexception ~ServerVersion;
%noexception ~ServerRouting;
%noexception ~DatetimeAndTimeZone;
%noexception ~DatetimeInNanos;
%noexception ~Decimal;
%noexception ~Duration;
%noexception ~StringAndOptValue;
%noexception ~Value;
%noexception ~GivenRowsHeaderBuilder;
%noexception ~GivenRowsHeader;
%noexception ~GivenRowsBuilder;
%noexception ~GivenRows;
%noexception ~ValueType;
%noexception ~BoolPromise;
%noexception ~VoidPromise;
%noexception ~AnalyzedQueryPromise;
%noexception ~QueryAnswerPromise;
%noexception ~StringPromise;

/* Analyze destructors do not throw */
%noexception ~AnalyzedQuery;
%noexception ~Conjunction;
%noexception ~ConjunctionID;
%noexception ~ConstraintWithSpan;
%noexception ~ConstraintVertex;
%noexception ~Fetch;
%noexception ~Function;
%noexception ~NamedRole;
%noexception ~PipelineStage;
%noexception ~Pipeline;
%noexception ~Reducer;
%noexception ~ReduceAssignment;
%noexception ~ReturnOperation;
%noexception ~ConstraintWithSpanIterator;
%noexception ~ConjunctionIDIterator;
%noexception ~ConstraintVertexIterator;
%noexception ~FunctionIterator;
%noexception ~PipelineStageIterator;
%noexception ~ReducerIterator;
%noexception ~ReduceAssignmentIterator;
%noexception ~SortVariable;
%noexception ~SortVariableIterator;
%noexception ~VariableAnnotations;
%noexception ~VariableAnnotationsIterator;
%noexception ~Variable;
%noexception ~VariableIterator;

%noexception given_rows_header_builder_new;
%noexception given_rows_header_builder_push;
%noexception given_rows_header_builder_finish;
%noexception given_rows_builder_new;
%noexception given_rows_builder_start_new_row;

/* Commit and rollback are non-throwing operations on construction */
%noexception transaction_commit;
%noexception transaction_rollback;

%define %promise(Prefix, Type, function_prefix)
%typemap(csbody) Prefix ## Promise %{
    private global::System.Runtime.InteropServices.HandleRef swigCPtr;
    protected bool swigCMemOwn;

    internal $csclassname(global::System.IntPtr cPtr, bool cMemoryOwn)
    {
        swigCMemOwn = cMemoryOwn;
        swigCPtr = new global::System.Runtime.InteropServices.HandleRef(this, cPtr);
    }

    internal static global::System.Runtime.InteropServices.HandleRef getCPtr($csclassname obj)
    {
        return (obj == null)
            ? new global::System.Runtime.InteropServices.HandleRef(null, global::System.IntPtr.Zero)
            : obj.swigCPtr;
    }

    public Type Resolve()
    {
        swigCMemOwn = false;
        return typedb_driver.function_prefix ## _promise_resolve(this);
    }
%}

%typemap(csdisposing, methodname="Dispose", methodmodifiers="protected", parameters="bool disposing") Prefix ## Promise %{{
    lock(this)
    {
        if (swigCPtr.Handle != global::System.IntPtr.Zero)
        {
            if (swigCMemOwn)
            {
                swigCMemOwn = false;
                Resolve(); // An equivalent for the standard "delete_xxxPromise".
            }

            swigCPtr = new global::System.Runtime.InteropServices.HandleRef(null, global::System.IntPtr.Zero);
        }
    }
}%}
%enddef

/* 3.0 API promises */
%promise(AnalyzedQuery, AnalyzedQuery, analyzed_query)
%promise(QueryAnswer, QueryAnswer, query_answer)
%promise(String, string, string)
%promise(Bool, bool, bool)

/* void promises require special handling */
%typemap(csbody) VoidPromise %{
    private global::System.Runtime.InteropServices.HandleRef swigCPtr;
    protected bool swigCMemOwn;

    internal $csclassname(global::System.IntPtr cPtr, bool cMemoryOwn)
    {
        swigCMemOwn = cMemoryOwn;
        swigCPtr = new global::System.Runtime.InteropServices.HandleRef(this, cPtr);
    }

    internal static global::System.Runtime.InteropServices.HandleRef getCPtr($csclassname obj)
    {
        return (obj == null)
            ? new global::System.Runtime.InteropServices.HandleRef(null, global::System.IntPtr.Zero)
            : obj.swigCPtr;
    }

    public void Resolve()
    {
        swigCMemOwn = false;
        typedb_driver.void_promise_resolve(this);
    }
%}

%typemap(csdisposing, methodname="Dispose", methodmodifiers="protected", parameters="bool disposing") VoidPromise %{{
    lock(this)
    {
        if (swigCPtr.Handle != global::System.IntPtr.Zero)
        {
            if (swigCMemOwn)
            {
                swigCMemOwn = false;
                Resolve(); // An equivalent for the standard "delete_VoidPromise".
            }

            swigCPtr = new global::System.Runtime.InteropServices.HandleRef(null, global::System.IntPtr.Zero);
        }
    }
}%}

%define %iterator(Type, CapitalizedType, FunctionPrefix)
%typemap(csinterfaces) CapitalizedType ## Iterator "System.Collections.Generic.IEnumerator<Type>";
%typemap(cscode) CapitalizedType ## Iterator %{
    private bool _isFinished = false;
    private Type? _current = null;

    object System.Collections.IEnumerator.Current
    {
        get
        {
            return Current;
        }
    }

    public Type Current
    {
        get
        {
            if (_current == null)
            {
                throw new System.IndexOutOfRangeException("Enumerator's Current element is out of range");
            }

            return _current;
        }
    }

    public bool MoveNext()
    {
        if (_isFinished)
        {
            return false;
        }

        _current = typedb_driver.FunctionPrefix ## _iterator_next(this);
        if (_current == null)
        {
            _isFinished = true;
            return false;
        }

        return true;
    }

    public void Reset()
    {
        throw new System.NotSupportedException("This Enumerator does not support Reset");
    }
%}
%enddef

/* 3.0 API iterators */
%iterator(Concept, Concept, concept)
%iterator(ConceptRow, ConceptRow, concept_row)
%iterator(string, String, string)
%iterator(StringAndOptValue, StringAndOptValue, string_and_opt_value)
%iterator(User, User, user)
%iterator(Database, Database, database)
%iterator(Server, Server, server)

/* Analyze API iterators */
%iterator(Variable, Variable, variable)
%iterator(PipelineStage, PipelineStage, pipeline_stage)
%iterator(ConjunctionID, ConjunctionID, conjunction_id)
%iterator(ConstraintWithSpan, ConstraintWithSpan, constraint_with_span)
%iterator(ConstraintVertex, ConstraintVertex, constraint_vertex)
%iterator(Function, Function, function)
%iterator(SortVariable, SortVariable, sort_variable)
%iterator(ReduceAssignment, ReduceAssignment, reduce_assignment)
%iterator(Reducer, Reducer, reducer)
%iterator(VariableAnnotations, VariableAnnotations, variable_annotations)


%define %array(Type)
%typemap(ctype) Type ** "Type**"
%typemap(cstype) Type ** "Type[]"
%typemap(imtype) Type ** "global::System.IntPtr"

%typemap(
    csin,
    pre="var unmanaged$csinput = Type.ArrayUnmanaged($csinput);",
    post="global::System.Runtime.InteropServices.Marshal.FreeHGlobal(unmanaged$csinput);"
) Type ** "unmanaged$csinput"

%typemap(csout) Type **
{
    return $imcall;
}

%typemap(in) char ** "$1 = $input;"
%typemap(freearg) char ** ""

%typemap(cscode) Type %{
    public static global::System.IntPtr ArrayUnmanaged($csclassname[] input)
    {
        int arraySize = input.Length;
        global::System.IntPtr unmanagedArray = System.Runtime.InteropServices.Marshal.AllocHGlobal(
            (arraySize + 1) * System.Runtime.InteropServices.Marshal.SizeOf<global::System.IntPtr>());

        unsafe
        {
            global::System.IntPtr* arrayPtr = (global::System.IntPtr*)unmanagedArray.ToPointer();

            for (int i = 0; i < arraySize; i++)
            {
                arrayPtr[i] = Type.getCPtr(input[i]).Handle;
            }

            arrayPtr[arraySize] = global::System.IntPtr.Zero;
        }

        return unmanagedArray;
    }
%}
%enddef

/* 3.0 API arrays */
%array(Concept)


%typemap(ctype) char ** "char**"
%typemap(cstype) char ** "string[]"
%typemap(imtype) char ** "global::System.IntPtr"

%typemap(
    csin,
    pre="
        int arraySize$csinput = $csinput.Length;
        global::System.IntPtr unmanaged$csinput = System.Runtime.InteropServices.Marshal.AllocHGlobal(
            (arraySize$csinput + 1) * System.Runtime.InteropServices.Marshal.SizeOf<global::System.IntPtr>());

        unsafe
        {
            global::System.IntPtr* arrayPtr = (global::System.IntPtr*)unmanaged$csinput.ToPointer();

            for (int i = 0; i < arraySize$csinput; i++)
            {
                arrayPtr[i] = global::System.Runtime.InteropServices.Marshal.StringToCoTaskMemAnsi($csinput[i]);
            }

            arrayPtr[arraySize$csinput] = global::System.IntPtr.Zero;
        }",
    post="
        unsafe
        {
            global::System.IntPtr* arrayPtr = (global::System.IntPtr*)unmanaged$csinput.ToPointer();

            for (int i = 0; i < arraySize$csinput; i++)
            {
                global::System.Runtime.InteropServices.Marshal.FreeHGlobal(arrayPtr[i]);
            }
        }

        global::System.Runtime.InteropServices.Marshal.FreeHGlobal(unmanaged$csinput);"
) char ** "unmanaged$csinput"

%typemap(csout) char **
{
    return $imcall;
}

%typemap(in) char ** "$1 = $input;"
%typemap(freearg) char ** ""


/* QueryAnswer needs custom handling because into_rows/into_documents consume ownership */
%typemap(csbody) QueryAnswer %{
    private global::System.Runtime.InteropServices.HandleRef swigCPtr;
    protected bool swigCMemOwn;

    internal $csclassname(global::System.IntPtr cPtr, bool cMemoryOwn)
    {
        swigCMemOwn = cMemoryOwn;
        swigCPtr = new global::System.Runtime.InteropServices.HandleRef(this, cPtr);
    }

    internal static global::System.Runtime.InteropServices.HandleRef getCPtr($csclassname obj)
    {
        return (obj == null)
            ? new global::System.Runtime.InteropServices.HandleRef(null, global::System.IntPtr.Zero)
            : obj.swigCPtr;
    }

    public ConceptRowIterator IntoRows()
    {
        swigCMemOwn = false;
        return typedb_driver.query_answer_into_rows(this);
    }

    public StringIterator IntoDocuments()
    {
        swigCMemOwn = false;
        return typedb_driver.query_answer_into_documents(this);
    }
%}

%typemap(csdisposing, methodname="Dispose", methodmodifiers="protected", parameters="bool disposing") QueryAnswer %{{
    lock(this)
    {
        if (swigCPtr.Handle != global::System.IntPtr.Zero)
        {
            if (swigCMemOwn)
            {
                swigCMemOwn = false;
                typedb_driverPINVOKE.delete_QueryAnswer(swigCPtr);
            }

            swigCPtr = new global::System.Runtime.InteropServices.HandleRef(null, global::System.IntPtr.Zero);
        }
    }
}%}
