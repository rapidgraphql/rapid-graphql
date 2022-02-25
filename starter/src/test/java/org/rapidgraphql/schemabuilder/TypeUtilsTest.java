package org.rapidgraphql.schemabuilder;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.rapidgraphql.schemabuilder.TypeUtils.isListType;
import static org.junit.jupiter.api.Assertions.*;

class TypeUtilsTest {
    @Test
    public void validatesTypeIsList() {
        assertTrue(isListType(List.class));
        assertTrue(isListType(ArrayList.class));
    }

}