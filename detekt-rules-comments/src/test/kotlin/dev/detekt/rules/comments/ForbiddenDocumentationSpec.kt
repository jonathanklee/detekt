package dev.detekt.rules.comments

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

private const val VISIBILITIES = "visibilities"

class ForbiddenDocumentationSpec {
    private val subject = ForbiddenDocumentation(Config.empty)

    @Test
    fun `reports a KDoc comment on a public function`() {
        val code = """
            /**
             * Builds the value of the Authorization header.
             */
            fun buildAuthorizationHeader(token: String) = token
        """.trimIndent()
        assertThat(subject.lint(code)).hasSize(1)
    }

    @Test
    fun `reports a KDoc comment on a class, a property and a nested function`() {
        val code = """
            /**
             * A session.
             */
            class Session {
                /**
                 * The token.
                 */
                val token: String = ""

                /**
                 * Refreshes the token.
                 */
                private fun refresh() {}
            }
        """.trimIndent()
        assertThat(subject.lint(code)).hasSize(3)
    }

    @Test
    fun `reports a KDoc comment on a typealias and on an object`() {
        val code = """
            /**
             * A token.
             */
            typealias Token = String

            /**
             * The registry.
             */
            object Registry
        """.trimIndent()
        assertThat(subject.lint(code)).hasSize(2)
    }

    @Test
    fun `points at the name of the declaration`() {
        val code = """
            /**
             * Builds the value of the Authorization header.
             */
            fun buildAuthorizationHeader(token: String) = token
        """.trimIndent()
        val findings = subject.lint(code)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).contains("buildAuthorizationHeader")
    }

    @Test
    fun `does not report a regular block comment`() {
        val code = """
            /* RFC 6750 wants the Bearer prefix. */
            fun buildAuthorizationHeader(token: String) = token
        """.trimIndent()
        assertThat(subject.lint(code)).isEmpty()
    }

    @Test
    fun `does not report a single line comment`() {
        val code = """
            // RFC 6750 wants the Bearer prefix.
            fun buildAuthorizationHeader(token: String) = token
        """.trimIndent()
        assertThat(subject.lint(code)).isEmpty()
    }

    @Test
    fun `does not report an undocumented declaration`() {
        val code = """
            fun buildAuthorizationHeader(token: String) = token
        """.trimIndent()
        assertThat(subject.lint(code)).isEmpty()
    }

    @Test
    fun `reports only the configured visibilities`() {
        val code = """
            /**
             * A session.
             */
            class Session {
                /**
                 * The token.
                 */
                internal val token: String = ""

                /**
                 * Refreshes the token.
                 */
                private fun refresh() {}
            }
        """.trimIndent()
        val rule = ForbiddenDocumentation(TestConfig(VISIBILITIES to listOf("internal", "private")))
        assertThat(rule.lint(code)).hasSize(2)
    }

    @Test
    fun `reports a protected declaration when protected is configured`() {
        val code = """
            open class Session {
                /**
                 * Refreshes the token.
                 */
                protected fun refresh() {}
            }
        """.trimIndent()
        val rule = ForbiddenDocumentation(TestConfig(VISIBILITIES to listOf("protected")))
        assertThat(rule.lint(code)).hasSize(1)
    }

    @Test
    fun `reports nothing when no visibility is configured`() {
        val code = """
            /**
             * Builds the value of the Authorization header.
             */
            fun buildAuthorizationHeader(token: String) = token
        """.trimIndent()
        val rule = ForbiddenDocumentation(TestConfig(VISIBILITIES to emptyList<String>()))
        assertThat(rule.lint(code)).isEmpty()
    }

    @Test
    fun `fails on an unknown visibility`() {
        val code = """
            /**
             * Builds the value of the Authorization header.
             */
            fun buildAuthorizationHeader(token: String) = token
        """.trimIndent()
        val rule = ForbiddenDocumentation(TestConfig(VISIBILITIES to listOf("package-private")))
        assertThatThrownBy { rule.lint(code) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("package-private")
    }
}
