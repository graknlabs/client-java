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

%module(threads=1) typedb_driver
%module(directors="1") typedb_driver
%{
extern "C" {
#include "typedb_driver.h"
}
%}
%include "stdint.i"
%include "carrays.i"
%include "typemaps.i"

#ifdef SWIGJAVA
%include "swig/typedb_driver_java.swg"
#endif

#ifdef SWIGPYTHON
%include "swig/typedb_driver_python.swg"
#endif

#ifdef SWIGCSHARP
%include "swig/typedb_driver_csharp.swg"
#endif

%nodefaultctor;


%define %dropproxy(Type, function_prefix)
struct Type {};
%ignore function_prefix ## _drop;
%extend Type { ~Type() { function_prefix ## _drop(self); } }
%enddef

%define %dropproxydefined(Type, function_prefix)
%ignore function_prefix ## _drop;
%extend Type { ~Type() { function_prefix ## _drop(self); } }
%enddef

%dropproxy(Error, error)

%dropproxy(Credentials, credentials)
%dropproxy(DriverOptions, driver_options)
%dropproxy(DriverTlsConfig, driver_tls_config)
%dropproxy(TransactionOptions, transaction_options)
%dropproxy(QueryOptions, query_options)
%dropproxydefined(ServerVersion, server_version)

#define transaction_drop transaction_drop_sync
#define database_drop database_close

#define typedb_driver_drop driver_close
%dropproxy(TypeDBDriver, typedb_driver)
%dropproxy(Transaction, transaction)

%dropproxy(Database, database)
%dropproxy(DatabaseIterator, database_iterator)
%dropproxy(Server, server)
%dropproxy(ServerIterator, server_iterator)

%dropproxy(User, user)
%dropproxy(UserIterator, user_iterator)

%dropproxy(Concept, concept)
%dropproxy(ConceptIterator, concept_iterator)

%dropproxy(ConceptRow, concept_row)
%dropproxy(ConceptRowIterator, concept_row_iterator)
%dropproxydefined(ServerRouting, server_routing)

%dropproxydefined(DatetimeAndTimeZone, datetime_and_time_zone)
%dropproxydefined(StringAndOptValue, string_and_opt_value)
%dropproxy(StringAndOptValueIterator, string_and_opt_value_iterator)
%dropproxy(StringIterator, string_iterator)

%dropproxy(QueryAnswer, query_answer)

%dropproxy(GivenRows, given_rows)
%dropproxy(GivenRowsBuilder, given_rows_builder)
%dropproxy(GivenRowsHeaderBuilder, given_rows_header_builder)
%dropproxy(GivenRowsHeader, given_rows_header)

%dropproxy(AnalyzedQuery, analyzed_query)
%dropproxy(Conjunction, conjunction)
%dropproxy(ConjunctionID, conjunction_id)
%dropproxy(ConstraintWithSpan, constraint_with_span)
%dropproxy(ConstraintVertex, constraint_vertex)
%dropproxy(Fetch, fetch)
%dropproxy(Function, function)
%dropproxy(Given, given)
%dropproxy(NamedRole, named_role)
%dropproxy(Pipeline, pipeline)
%dropproxy(PipelineStage, pipeline_stage)
%dropproxy(ReduceAssignment, reduce_assignment)
%dropproxy(ReturnOperation, return_operation)
%dropproxy(Reducer, reducer)
%dropproxy(SortVariable, sort_variable)
%dropproxy(VariableAnnotations, variable_annotations)
%dropproxy(Variable, variable)

%dropproxy(ConjunctionIDIterator, conjunction_id_iterator)
%dropproxy(ConstraintWithSpanIterator, constraint_with_span_iterator)
%dropproxy(ConstraintVertexIterator, constraint_vertex_iterator)
%dropproxy(FunctionIterator, function_iterator)
%dropproxy(PipelineStageIterator, pipeline_stage_iterator)
%dropproxy(ReduceAssignmentIterator, reduce_assignment_iterator)
%dropproxy(ReducerIterator, reducer_iterator)
%dropproxy(SortVariableIterator, sort_variable_iterator)
%dropproxy(VariableAnnotationsIterator, variable_annotations_iterator)
%dropproxy(VariableIterator, variable_iterator)

%define %promiseproxy(Type, function_prefix)
struct Type {};
%newobject function_prefix ## _resolve;
%delobject function_prefix ## _resolve;
%extend Type { ~Type() { function_prefix ## _drop(self); } }
%delobject function_prefix ## _drop;
%enddef

%promiseproxy(AnalyzedQueryPromise, analyzed_query_promise)
%promiseproxy(BoolPromise, bool_promise)
%promiseproxy(StringPromise, string_promise)
%promiseproxy(QueryAnswerPromise, query_answer_promise)
%promiseproxy(VoidPromise, void_promise)

%feature("director") TransactionCallbackDirector;
%inline %{
struct TransactionCallbackDirector {
    TransactionCallbackDirector() {}
    virtual ~TransactionCallbackDirector() {}
    virtual void callback(Error*) = 0;
};
%}

%{
#include <memory>
#include <iostream>
#include <mutex>
#include <atomic>
#include <unordered_map>

// Thread-safe map for transaction callback directors
class ThreadSafeTransactionCallbacks {
private:
    static std::unordered_map<size_t, TransactionCallbackDirector*> s_callbacks;
    static std::mutex s_mutex;

public:
    ThreadSafeTransactionCallbacks(const ThreadSafeTransactionCallbacks&) = delete;
    ThreadSafeTransactionCallbacks& operator=(const ThreadSafeTransactionCallbacks&) = delete;

    static void insert(size_t key, TransactionCallbackDirector* value) {
        std::lock_guard<std::mutex> lock(s_mutex);
        s_callbacks[key] = value;
    }

    static TransactionCallbackDirector* find(size_t key) {
        std::lock_guard<std::mutex> lock(s_mutex);
        auto it = s_callbacks.find(key);
        return (it != s_callbacks.end()) ? it->second : nullptr;
    }

    static void remove(size_t key) {
        std::lock_guard<std::mutex> lock(s_mutex);
        s_callbacks.erase(key);
    }
};

std::unordered_map<size_t, TransactionCallbackDirector*> ThreadSafeTransactionCallbacks::s_callbacks;
std::mutex ThreadSafeTransactionCallbacks::s_mutex;

static void transaction_callback_execute(size_t ID, Error* error) {
    try {
        auto cb = ThreadSafeTransactionCallbacks::find(ID);
        if (cb != nullptr) {
            cb->callback(error);
        }
        ThreadSafeTransactionCallbacks::remove(ID);
    } catch (std::exception const& e) {
        std::cerr << "[ERROR] Exception in transaction callback: " << e.what() << std::endl;
    }
}
%}

%rename(transaction_on_close) transaction_on_close_register;
%ignore transaction_on_close;
%inline %{
VoidPromise* transaction_on_close_register(const Transaction* transaction, TransactionCallbackDirector* handler) {
    static std::atomic_size_t nextID;
    std::size_t ID = nextID.fetch_add(1);
    ThreadSafeTransactionCallbacks::insert(ID, handler);
    return transaction_on_close(transaction, ID, &transaction_callback_execute);
}
%}

%newobject transaction_new;
%newobject transaction_query;
%newobject transaction_query_given_rows;
%delobject transaction_commit;

%typemap(newfree) char* "string_free($1);";
%ignore string_free;

%newobject concept_row_get;
%newobject concept_row_get_column_names;
%newobject concept_row_get_query_type;
%newobject concept_row_involved_conjunctions;
%newobject query_answer_get_query_type;
%newobject concept_row_get_concepts;
%newobject concept_row_get_index;
%newobject concept_row_get_query_structure;
%newobject concept_row_to_string;

%newobject query_answer_into_rows;
%newobject query_answer_into_documents;
%delobject query_answer_into_rows;
%delobject query_answer_into_documents;

%newobject concept_to_string;

%newobject entity_get_type;
%newobject relation_get_type;
%newobject attribute_get_type;

%newobject concept_new_boolean;
%newobject concept_new_integer;
%newobject concept_new_double;
%newobject concept_new_decimal;
%newobject concept_new_decimal_from_string;
%newobject concept_new_string;
%newobject concept_new_date_from_seconds;
%newobject concept_new_datetime;
%newobject concept_new_datetime_tz_iana;
%newobject concept_new_datetime_tz_offset;
%newobject concept_new_duration;

%newobject concept_get_label;
%newobject concept_try_get_label;
%newobject concept_try_get_iid;
%newobject concept_try_get_value_type;
%newobject concept_try_get_value;
%newobject credentials_new;
%newobject server_routing_auto;
%newobject server_routing_direct;

%newobject driver_new;
%newobject driver_new_with_addresses;
%newobject driver_new_with_address_translation;
%newobject driver_server_version;
%newobject driver_primary_server;
%newobject driver_servers;

%newobject driver_options_new;
%newobject driver_options_get_tls_config;
%newobject driver_tls_config_new_disabled;
%newobject driver_tls_config_new_enabled_with_native_root_ca;
%newobject driver_tls_config_new_enabled_with_root_ca_path;
%newobject driver_tls_config_get_root_ca_path;
%newobject database_get_name;
%newobject database_schema;
%newobject database_type_schema;
%delobject database_delete;

%newobject server_get_address;

%newobject databases_all;
%newobject databases_get;

%newobject given_rows_header_builder_new;
%delobject given_rows_header_builder_finish;
%newobject given_rows_header_builder_finish;
%newobject given_rows_builder_new;
%delobject given_rows_builder_finish;
%newobject given_rows_builder_finish;

%newobject analyzed_query_pipeline;
%newobject analyzed_preamble;
%newobject analyzed_fetch;
%newobject analyzed_given;
%newobject conjunction_get_variable_annotations;
%newobject conjunction_get_constraints;
%newobject conjunction_id_string_repr;
%newobject constraint_span_begin;
%newobject constraint_span_end;
%newobject constraint_variant;
%newobject constraint_isa_get_instance;
%newobject constraint_isa_get_type;
%newobject constraint_has_get_owner;
%newobject constraint_has_get_attribute;
%newobject constraint_links_get_relation;
%newobject constraint_links_get_player;
%newobject constraint_links_get_role;
%newobject constraint_sub_get_subtype;
%newobject constraint_sub_get_supertype;
%newobject constraint_owns_get_owner;
%newobject constraint_owns_get_attribute;
%newobject constraint_relates_get_relation;
%newobject constraint_relates_get_role;
%newobject constraint_plays_get_player;
%newobject constraint_plays_get_role;
%newobject constraint_isa_get_exactness;
%newobject constraint_has_get_exactness;
%newobject constraint_links_get_exactness;
%newobject constraint_sub_get_exactness;
%newobject constraint_owns_get_exactness;
%newobject constraint_relates_get_exactness;
%newobject constraint_plays_get_exactness;
%newobject constraint_function_call_get_name;
%newobject constraint_function_call_get_assigned;
%newobject constraint_function_call_get_arguments;
%newobject constraint_expression_get_text;
%newobject constraint_expression_get_assigned;
%newobject constraint_expression_get_arguments;
%newobject constraint_is_get_lhs;
%newobject constraint_is_get_rhs;
%newobject constraint_iid_get_variable;
%newobject constraint_iid_get_iid;
%newobject constraint_delete_concepts_get_variables;
%newobject constraint_comparison_get_lhs;
%newobject constraint_comparison_get_rhs;
%newobject constraint_comparison_get_comparator;
%newobject constraint_kind_get_kind;
%newobject constraint_kind_get_type;
%newobject constraint_label_get_variable;
%newobject constraint_label_get_label;
%newobject constraint_value_get_attribute_type;
%newobject constraint_value_get_value_type;
%newobject constraint_isset_get_variables
%newobject constraint_or_get_branches;
%newobject constraint_not_get_conjunction;
%newobject constraint_try_get_conjunction;
%newobject constraint_string_repr;
%newobject constraint_vertex_variant;
%newobject constraint_vertex_as_variable;
%newobject constraint_vertex_as_label;
%newobject constraint_vertex_as_value;
%newobject constraint_vertex_as_named_role;
%newobject constraint_with_span_iterator_next;
%newobject constraint_with_span_iterator_drop;
%newobject constraint_vertex_iterator_next;
%newobject constraint_vertex_iterator_drop;
%newobject constraint_with_span_drop;
%newobject constraint_vertex_drop;
%newobject named_role_get_name;
%newobject named_role_get_variable;
%newobject named_role_string_repr;
%newobject function_argument_variables;
%newobject function_argument_annotations;
%newobject function_body;
%newobject function_return_operation;
%newobject function_return_annotations;
%newobject fetch_leaf_annotations;
%newobject fetch_list_element;
%newobject fetch_object_fields;
%newobject fetch_object_get_field;
%newobject given_variables;
%newobject given_variable_annotations;

%newobject pipeline_get_conjunction;
%newobject pipeline_stages;
%newobject pipeline_stage_get_block;
%newobject pipeline_stage_delete_get_deleted_variables;
%newobject pipeline_stage_reduce_get_reducer_assignments;
%newobject pipeline_stage_sort_get_sort_variables;
%newobject pipeline_stage_reduce_get_groupby;
%newobject pipeline_stage_string_repr;
%newobject sort_variable_get_variable;
%newobject reduce_assignment_get_assigned;
%newobject reduce_assignment_get_reducer;
%newobject reducer_get_name;
%newobject reducer_get_arguments;
%newobject variable_get_name;
%newobject variable_string_repr;

%newobject function_iterator_next;
%newobject conjunction_id_iterator_next;
%newobject constraint_with_span_iterator_next;
%newobject constraint_vertex_iterator_next;
%newobject pipeline_stage_iterator_next;
%newobject reduce_assignment_iterator_next;
%newobject reducer_iterator_next;
%newobject sort_variable_iterator_next;
%newobject variable_annotations_iterator_next;
%newobject variable_iterator_next;


%newobject get_last_error;
%newobject error_code;
%newobject error_message;

%newobject transaction_options_new;
%newobject query_options_new;

%newobject concept_iterator_next;
%newobject concept_row_iterator_next;
%newobject database_iterator_next;
%newobject server_iterator_next;
%newobject string_iterator_next;
%newobject string_and_opt_value_iterator_next;
%newobject user_iterator_next;
%newobject variable_iterator_next;

%newobject users_all;
%newobject users_get_current;
%newobject users_get;
%newobject user_get_name;
%delobject user_delete;

%include "typedb_driver.h"
