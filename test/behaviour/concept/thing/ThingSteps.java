/*
 * Copyright (C) 2020 Grakn Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package grakn.client.test.behaviour.concept.thing;

import grakn.client.concept.thing.Thing;
import grakn.client.concept.type.ThingType;
import grakn.client.test.behaviour.config.Parameters.RootLabel;
import grakn.client.test.behaviour.config.Parameters.ScopedLabel;
import io.cucumber.java.After;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.HashMap;
import java.util.Map;

import static grakn.client.test.behaviour.concept.type.thingtype.ThingTypeSteps.get_thing_type;
import static grakn.client.test.behaviour.connection.ConnectionSteps.tx;
import static grakn.client.test.behaviour.util.Util.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ThingSteps {

    private static Map<String, Thing.Remote> things = new HashMap<>();

    public static Thing.Remote get(String variable) {
        return things.get(variable);
    }

    public static void put(String variable, Thing.Remote thing) {
        things.put(variable, thing);
    }

    public static void remove(String variable) {
        things.remove(variable);
    }

    @Then("entity/attribute/relation {var} is null: {bool}")
    public void thing_is_null(String var, boolean isNull) {
        if (isNull) {
            assertNull(get(var));
        } else {
            assertNotNull(get(var));
        }
    }

    @Then("entity/attribute/relation {var} is deleted: {bool}")
    public void thing_is_deleted(String var, boolean isDeleted) {
        assertEquals(isDeleted, get(var).isDeleted());
    }

    @Then("{root_label} {var} has type: {type_label}")
    public void thing_has_type(RootLabel rootLabel, String var, String typeLabel) {
        ThingType.Remote type = get_thing_type(rootLabel, typeLabel);
        assertEquals(type, get(var).getType());
    }

    @When("delete entity:/attribute:/relation: {var}")
    public void delete_thing(String var) {
        get(var).delete();
    }

    @When("entity/attribute/relation {var} set has: {var}")
    public void thing_set_has(String var1, String var2) {
        get(var1).setHas(get(var2).asAttribute());
    }

    @Then("entity/attribute/relation {var} set has: {var}; throws exception")
    public void thing_set_has_throws_exception(String var1, String var2) {
        assertThrows(() -> get(var1).setHas(get(var2).asAttribute()));
    }

    @When("entity/attribute/relation {var} unset has: {var}")
    public void thing_remove_has(String var1, String var2) {
        get(var1).unsetHas(get(var2).asAttribute());
    }

    @Then("entity/attribute/relation {var} get keys contain: {var}")
    public void thing_get_keys_contain(String var1, String var2) {
        assertTrue(get(var1).getHas(true).anyMatch(k -> k.equals(get(var2))));
    }

    @Then("entity/attribute/relation {var} get keys do not contain: {var}")
    public void thing_get_keys_do_not_contain(String var1, String var2) {
        assertTrue(get(var1).getHas(true).noneMatch(k -> k.equals(get(var2))));
    }

    @Then("entity/attribute/relation {var} get attributes contain: {var}")
    public void thing_get_attributes_contain(String var1, String var2) {
        assertTrue(get(var1).getHas().anyMatch(k -> k.equals(get(var2))));
    }

    @Then("entity/attribute/relation {var} get attributes\\( ?{type_label} ?) contain: {var}")
    public void thing_get_attributes_contain(String var1, String typeLabel, String var2) {
        assertTrue(get(var1).getHas(tx().getAttributeType(typeLabel)).anyMatch(k -> k.equals(get(var2))));
    }

    @Then("entity/attribute/relation {var} get attributes\\( ?{type_label} ?) as\\( ?boolean ?) contain: {var}")
    public void thing_get_attributes_as_boolean_contain(String var1, String typeLabel, String var2) {
        assertTrue(get(var1).getHas(tx().getAttributeType(typeLabel).asBoolean()).anyMatch(k -> k.equals(get(var2))));
    }

    @Then("entity/attribute/relation {var} get attributes\\( ?{type_label} ?) as\\( ?long ?) contain: {var}")
    public void thing_get_attributes_as_long_contain(String var1, String typeLabel, String var2) {
        assertTrue(get(var1).getHas(tx().getAttributeType(typeLabel).asLong()).anyMatch(k -> k.equals(get(var2))));
    }

    @Then("entity/attribute/relation {var} get attributes\\( ?{type_label} ?) as\\( ?double ?) contain: {var}")
    public void thing_get_attributes_as_double_contain(String var1, String typeLabel, String var2) {
        assertTrue(get(var1).getHas(tx().getAttributeType(typeLabel).asDouble()).anyMatch(k -> k.equals(get(var2))));
    }

    @Then("entity/attribute/relation {var} get attributes\\( ?{type_label} ?) as\\( ?string ?) contain: {var}")
    public void thing_get_attributes_as_string_contain(String var1, String typeLabel, String var2) {
        assertTrue(get(var1).getHas(tx().getAttributeType(typeLabel).asString()).anyMatch(k -> k.equals(get(var2))));
    }

    @Then("entity/attribute/relation {var} get attributes\\( ?{type_label} ?) as\\( ?datetime ?) contain: {var}")
    public void thing_get_attributes_as_datetime_contain(String var1, String typeLabel, String var2) {
        assertTrue(get(var1).getHas(tx().getAttributeType(typeLabel).asDateTime()).anyMatch(k -> k.equals(get(var2))));
    }

    @Then("entity/attribute/relation {var} get attributes do not contain: {var}")
    public void thing_get_attributes_do_not_contain(String var1, String var2) {
        assertTrue(get(var1).getHas().noneMatch(k -> k.equals(get(var2))));
    }

    @Then("entity/attribute/relation {var} get attributes\\( ?{type_label} ?) do not contain: {var}")
    public void thing_get_attributes_do_not_contain(String var1, String typeLabel, String var2) {
        assertTrue(get(var1).getHas(tx().getAttributeType(typeLabel)).noneMatch(k -> k.equals(get(var2))));
    }

    @Then("entity/attribute/relation {var} get attributes\\( ?{type_label} ?) as\\( ?boolean ?) do not contain: {var}")
    public void thing_get_attributes_as_boolean_do_not_contain(String var1, String typeLabel, String var2) {
        assertTrue(get(var1).getHas(tx().getAttributeType(typeLabel).asBoolean()).noneMatch(k -> k.equals(get(var2))));
    }

    @Then("entity/attribute/relation {var} get attributes\\( ?{type_label} ?) as\\( ?long ?) do not contain: {var}")
    public void thing_get_attributes_as_long_do_not_contain(String var1, String typeLabel, String var2) {
        assertTrue(get(var1).getHas(tx().getAttributeType(typeLabel).asLong()).noneMatch(k -> k.equals(get(var2))));
    }

    @Then("entity/attribute/relation {var} get attributes\\( ?{type_label} ?) as\\( ?double ?) do not contain: {var}")
    public void thing_get_attributes_as_double_do_not_contain(String var1, String typeLabel, String var2) {
        assertTrue(get(var1).getHas(tx().getAttributeType(typeLabel).asDouble()).noneMatch(k -> k.equals(get(var2))));
    }

    @Then("entity/attribute/relation {var} get attributes\\( ?{type_label} ?) as\\( ?string ?) do not contain: {var}")
    public void thing_get_attributes_as_string_do_not_contain(String var1, String typeLabel, String var2) {
        assertTrue(get(var1).getHas(tx().getAttributeType(typeLabel).asString()).noneMatch(k -> k.equals(get(var2))));
    }

    @Then("entity/attribute/relation {var} get attributes\\( ?{type_label} ?) as\\( ?datetime ?) do not contain: {var}")
    public void thing_get_attributes_as_datetime_do_not_contain(String var1, String typeLabel, String var2) {
        assertTrue(get(var1).getHas(tx().getAttributeType(typeLabel).asDateTime()).noneMatch(k -> k.equals(get(var2))));
    }

    @Then("entity/attribute/relation {var} get relations\\( ?{scoped_label} ?) contain: {var}")
    public void thing_get_relations_contain(String var1, ScopedLabel scopedLabel, String var2) {
        assertTrue(get(var1).getRelations(tx().getRelationType(scopedLabel.scope()).getRelates(scopedLabel.role()))
                           .anyMatch(k -> k.equals(get(var2))));
    }

    @Then("entity/attribute/relation {var} get relations contain: {var}")
    public void thing_get_relations_contain(String var1, String var2) {
        assertTrue(get(var1).getRelations().anyMatch(k -> k.equals(get(var2))));
    }

    @Then("entity/attribute/relation {var} get relations\\( ?{scoped_label} ?) do not contain: {var}")
    public void thing_get_relations_do_not_contain(String var1, ScopedLabel scopedLabel, String var2) {
        assertTrue(get(var1).getRelations(tx().getRelationType(scopedLabel.scope()).getRelates(scopedLabel.role()))
                           .noneMatch(k -> k.equals(get(var2))));
    }

    @Then("entity/attribute/relation {var} get relations do not contain: {var}")
    public void thing_get_relations_do_not_contain(String var1, String var2) {
        assertTrue(get(var1).getRelations().noneMatch(k -> k.equals(get(var2))));
    }

    @Then("root\\( ?thing ?) get instances count: {int}")
    public void root_thing_type_get_instances_contain(int count) {
        assertEquals(count, tx().getRootType().getInstances().count());
    }

    @Then("root\\( ?thing ?) get instances contain: {var}")
    public void root_thing_type_get_instances_contain(String var) {
        assertTrue(tx().getRootType().getInstances().anyMatch(i -> i.equals(get(var))));
    }

    @Then("root\\( ?thing ?) get instances is empty")
    public void root_thing_type_get_instances_is_empty() {
        assertEquals(0, tx().getRootType().getInstances().count());
    }

    @After
    public void clear() {
        things.clear();
    }
}
