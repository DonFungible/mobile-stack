// Mirrors apps/ios/.swiftlint.yml custom_rules: the design system is the only place allowed to touch
// raw Material controls, colors and text styles, and each cross-cutting concern has one seam.

data class Convention(
    val id: String,
    val pattern: Regex,
    val message: String,
    val appliesTo: Regex = Regex(".*"),
    val exemptPaths: Regex? = null,
)

val featureCode = Regex(".*/(features|navigation|common)/.*")

val conventions = listOf(
    Convention(
        id = "design-system-controls",
        pattern = Regex(
            """^import androidx\.compose\.material3\.""" +
                """(Text|Button|TextButton|OutlinedButton|IconButton|Switch|Checkbox|TextField|OutlinedTextField)$""",
        ),
        message = "Use AppText / AppButton / AppIconButton / AppToggle from designsystem, not raw Material controls.",
        appliesTo = featureCode,
    ),
    Convention(
        id = "design-token-colors",
        pattern = Regex(
            """Color\(0x|Color\.(Red|Blue|Green|Yellow|Cyan|Magenta|Gray|Black|White|DarkGray|LightGray)\b""",
        ),
        message = "Use an AppColor token. Add new colors to packages/design-tokens/tokens.json.",
        appliesTo = featureCode,
    ),
    Convention(
        id = "design-token-type",
        pattern = Regex("""\bTextStyle\(|fontSize\s*=|MaterialTheme\.typography"""),
        message = "Set type through AppText styles.",
        appliesTo = featureCode,
    ),
    Convention(
        id = "no-raw-clickable",
        pattern = Regex("""^import androidx\.compose\.foundation\.clickable$"""),
        message = "Use AppPressable from designsystem so press feedback and hit targets stay consistent.",
        appliesTo = featureCode,
    ),
    Convention(
        id = "no-android-log",
        pattern = Regex("""^import android\.util\.Log$|\bprintln\("""),
        message = "Use Log from core.services.",
        exemptPaths = Regex(".*/(core/services/Log|platform/SystemServices|cli/Main)\\.kt"),
    ),
    Convention(
        id = "networking-seam",
        pattern = Regex("""^import (okhttp3|java\.net\.HttpURLConnection)\b"""),
        message = "All requests go through ApiClient so auth, request ids and error reporting apply.",
        exemptPaths = Regex(".*/platform/.*"),
    ),
    Convention(
        id = "storage-seam",
        pattern = Regex("""^import android\.content\.SharedPreferences$|getSharedPreferences\("""),
        message = "Persist through the core's PreferencesStore so every key is in the StorageKey registry.",
        exemptPaths = Regex(".*/platform/.*"),
    ),
    Convention(
        id = "view-contract",
        pattern = Regex("""\bviewModel\.(?!state\b|send\()"""),
        message = "A screen's whole contract is: read viewModel.state, call viewModel.send(action).",
        appliesTo = featureCode,
    ),
    Convention(
        id = "no-ticket-references",
        pattern = Regex("""//.*\b(ENG|TICKET)-\d+"""),
        message = "Ticket ids belong in the commit and PR description, not in source.",
    ),
)

abstract class CheckConventions : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:Internal
    var rules: List<Convention> = emptyList()

    @TaskAction
    fun check() {
        val violations = sources.files.sorted().flatMap { file ->
            val path = file.invariantSeparatorsPath
            val active = rules.filter { it.appliesTo.matches(path) && it.exemptPaths?.matches(path) != true }
            file.readLines().flatMapIndexed { index, line ->
                active.filter { it.pattern.containsMatchIn(line.trim()) }
                    .map { "$path:${index + 1}: [${it.id}] ${it.message}" }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(violations.joinToString("\n", prefix = "Convention violations:\n"))
        }
    }
}

tasks.register<CheckConventions>("checkConventions") {
    group = "verification"
    description = "Enforces the design-system and seam conventions."
    sources.from(
        listOf("src", "../core/src", "../introspection/src", "../cli/src").map { root ->
            fileTree(root) {
                include("**/*.kt")
                exclude("**/generated/**")
            }
        },
    )
    rules = conventions
}
