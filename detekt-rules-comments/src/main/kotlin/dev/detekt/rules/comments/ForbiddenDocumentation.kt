package dev.detekt.rules.comments

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierTypeOrDefault

// Note: ​ (zero-width-space) is used to prevent the Kotlin parser getting confused by talking about KDoc in KDoc.

/**
 * Reports KDoc comments on declarations whose visibility is listed in `visibilities`.
 *
 * Not every codebase publishes generated documentation. Where none is published, a KDoc block is read
 * only by someone who already has the declaration in front of them, and it tends to restate the
 * signature (`@param`, `@return`) rather than add anything. Teams working this way want the meaning to
 * live in the names, and a regular comment where the code genuinely cannot carry it.
 *
 * By default every declaration is reported. Narrow `visibilities` to keep KDoc on the surface that is
 * actually published, for example `['internal', 'private']` for a library that documents its public API
 * only. Note that [DocumentationOverPrivateFunction] and [DocumentationOverPrivateProperty] already
 * cover private functions and properties, so leave those two inactive when using this rule with
 * `private` in the list.
 *
 * <noncompliant>
 * /​**
 *  * Builds the value of the Authorization header.
 *  *
 *  * @param token the access token
 *  * @return the header value
 *  *​/
 * fun buildAuthorizationHeader(token: String) = "Bearer " + token
 * </noncompliant>
 *
 * <compliant>
 * fun buildAuthorizationHeader(token: String) = "Bearer " + token
 * </compliant>
 */
class ForbiddenDocumentation(config: Config) :
    Rule(
        config,
        "KDoc comment in a codebase that does not publish generated documentation. " +
            "Prefer an expressive name, or a regular comment where the code cannot carry the explanation."
    ) {

    @Configuration("visibilities of the declarations for which a KDoc comment is reported")
    private val visibilities: Set<KtModifierKeywordToken> by config(
        listOf("public", "protected", "internal", "private")
    ) { values -> values.mapTo(mutableSetOf(), ::toVisibilityKeyword) }

    override fun visitDeclaration(dcl: KtDeclaration) {
        super.visitDeclaration(dcl)
        if (dcl.docComment == null) return
        if (dcl.visibilityModifierTypeOrDefault() !in visibilities) return

        report(Finding(dcl.toEntity(), dcl.toFindingMessage()))
    }
}

private fun KtDeclaration.toEntity(): Entity =
    if (this is KtNamedDeclaration) {
        Entity.atName(this)
    } else {
        Entity.from(this)
    }

private fun KtDeclaration.toFindingMessage(): String {
    val name = (this as? KtNamedDeclaration)?.nameAsName?.asString()
    return if (name == null) {
        "This declaration has a KDoc comment. Replace it with a regular comment or remove it."
    } else {
        "The declaration `$name` has a KDoc comment. Replace it with a regular comment, " +
            "or give it a name that makes the comment unnecessary."
    }
}

private val visibilityKeywords = listOf(
    KtTokens.PUBLIC_KEYWORD,
    KtTokens.PROTECTED_KEYWORD,
    KtTokens.INTERNAL_KEYWORD,
    KtTokens.PRIVATE_KEYWORD,
)

private fun toVisibilityKeyword(value: String): KtModifierKeywordToken {
    val allowed = visibilityKeywords.joinToString("|") { it.value }
    return visibilityKeywords.singleOrNull { it.value == value }
        ?: error("Unknown visibility `$value`, allowed values are: $allowed")
}
