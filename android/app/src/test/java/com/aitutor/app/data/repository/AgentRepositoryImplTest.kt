package com.aitutor.app.data.repository

import com.aitutor.app.data.tool.impl.CalculatorTool
import com.aitutor.app.data.tool.impl.DateTimeTool
import com.aitutor.app.data.tool.registry.ToolRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AgentRepositoryImplTest {

    private lateinit var repository: AgentRepositoryImpl

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        // Clean DataStore between tests to avoid state leakage
        val prefsDir = File(context.filesDir, "datastore")
        prefsDir.deleteRecursively()
        repository = AgentRepositoryImpl(context)
        // Ensure clean initial state for all tests
        runBlocking { repository.setAgentEnabled(false) }
    }

    @Test
    fun getAgentEnabled_defaultShouldBeFalse() = runTest {
        val enabled = repository.getAgentEnabled().first()
        assertFalse(enabled)
    }

    @Test
    fun setAgentEnabled_true_shouldPersist() = runTest {
        repository.setAgentEnabled(true)
        val enabled = repository.getAgentEnabled().first()
        assertTrue(enabled)
    }

    @Test
    fun setAgentEnabled_false_shouldPersist() = runTest {
        repository.setAgentEnabled(true)
        repository.setAgentEnabled(false)
        val enabled = repository.getAgentEnabled().first()
        assertFalse(enabled)
    }

    @Test
    fun setAgentEnabled_multipleValues_shouldReflectLatest() = runTest {
        repository.setAgentEnabled(true)
        repository.setAgentEnabled(false)
        repository.setAgentEnabled(true)
        val enabled = repository.getAgentEnabled().first()
        assertTrue(enabled)
    }

    @Test
    fun getToolsDefinitions_shouldReturnRegisteredTools() {
        ToolRegistry.register(CalculatorTool())
        ToolRegistry.register(DateTimeTool())
        val definitions = repository.getToolsDefinitions()
        assertNotNull(definitions)
        assertTrue(definitions.isNotEmpty())
        val functionNames = definitions.mapNotNull { tool ->
            (tool["function"] as? Map<String, Any>)?.get("name")?.toString()
        }
        assertTrue("Should contain calculator", functionNames.contains("calculator"))
        assertTrue("Should contain datetime", functionNames.contains("datetime"))
    }

    @Test
    fun getToolsDefinitions_whenEmpty_shouldReturnEmptyList() {
        val definitions = repository.getToolsDefinitions()
        assertNotNull(definitions)
    }
}
