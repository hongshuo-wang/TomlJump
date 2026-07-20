import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.tasks.SignPluginTask
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
}

val pluginDescription = """
    <p>
        TomlJump adds focused bidirectional navigation between TOML configuration, project files,
        and matching source configuration declarations.
    </p>
    <ul>
        <li>Jump from TOML string file paths to project files.</li>
        <li>Navigate both ways between TOML tables and keys and matching source declarations.</li>
        <li>Resolve conservative config declarations in Go, Python, Java, TypeScript, and JavaScript.</li>
        <li>Use standard JetBrains navigation actions such as Go to Declaration and command/control click.</li>
    </ul>
    <p>
        TomlJump intentionally does not replace TOML syntax highlighting, formatting, schema validation,
        or completion. It focuses on high-confidence navigation and stays quiet when a configuration
        relationship is not credible.
    </p>
    <hr/>
    <p>
        TomlJump 为 TOML 配置与对应的项目文件、源码配置声明提供专注的双向跳转能力。
    </p>
    <ul>
        <li>从 TOML 字符串文件路径跳转到项目文件。</li>
        <li>在 TOML table、key 与匹配的源码配置声明之间双向跳转。</li>
        <li>支持 Go、Python、Java、TypeScript、JavaScript 中的保守配置匹配。</li>
        <li>使用 JetBrains 原生跳转能力，例如 Go to Declaration 和 command/control click。</li>
    </ul>
    <p>
        TomlJump 不替代 TOML 语法高亮、格式化、schema 校验或补全。它只专注于高置信度导航；
        当配置关系不够可信时会静默跳过。
    </p>
""".trimIndent()

val pluginChangeNotes = """
    <ul>
        <li>新增嵌套 TOML 路径双向导航，使用最接近字段的明确配置容器匹配嵌套 table、key 和 root dotted key。</li>
        <li>改用 JetBrains TOML PSI 提取路径，支持 quoted key、dotted key、普通 table 和 array of tables。</li>
        <li>继续保持保守导航：dotted path 仅叶子段参与跳转，inline table 成员、畸形路径和容器不匹配保持静默。</li>
        <li>扩充可复用手测项目，覆盖新增语法、正反向跳转和不应跳转的负面案例。</li>
    </ul>
    <hr/>
    <ul>
        <li>Added bidirectional navigation for nested TOML paths, matching nested tables, keys, and root dotted keys through the nearest explicit source container.</li>
        <li>Moved path extraction to JetBrains TOML PSI with support for quoted keys, dotted keys, standard tables, and arrays of tables.</li>
        <li>Kept navigation conservative: only dotted-path leaf segments navigate, while inline-table members, malformed paths, and mismatched containers stay unresolved.</li>
        <li>Expanded the reusable manual demo with positive and negative coverage for the new syntax.</li>
    </ul>
""".trimIndent()

val verifierIdeType = providers.gradleProperty("verifierIde")
    .orElse("IC")
    .map(IntelliJPlatformType::fromCode)
val verifierIdeVersion = providers.gradleProperty("verifierIdeVersion")
    .orElse("2024.3")
val externalPluginArchive = providers.gradleProperty("pluginArchivePath")

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(project(":core:tomljump-core"))

    intellijPlatform {
        intellijIdeaCommunity("2024.3")
        bundledPlugin("org.toml.lang")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        id = "com.tomljump"
        name = "TomlJump"
        version = providers.gradleProperty("pluginVersion")
        description = pluginDescription
        changeNotes = pluginChangeNotes

        vendor {
            name = "harrison_wang"
            url = "https://github.com/hongshuo-wang/TomlJump"
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild.unsetConvention()
            providers.gradleProperty("pluginUntilBuild")
                .map(String::trim)
                .filter(String::isNotEmpty)
                .orNull
                ?.let { untilBuild = it }
        }
    }

    signing {
        certificateChain = providers.environmentVariable("JETBRAINS_CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("JETBRAINS_PRIVATE_KEY")
        password = providers.environmentVariable("JETBRAINS_PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("JETBRAINS_MARKETPLACE_TOKEN")
        channels = providers.environmentVariable("JETBRAINS_MARKETPLACE_CHANNEL")
            .map { listOf(it) }
            .orElse(listOf("default"))
    }

    pluginVerification {
        ides {
            create(verifierIdeType, verifierIdeVersion)
        }
    }
}

tasks.named<VerifyPluginTask>("verifyPlugin") {
    if (externalPluginArchive.isPresent) {
        archiveFile.set(rootProject.layout.projectDirectory.file(externalPluginArchive.get()))
    }
}

tasks.named<SignPluginTask>("signPlugin") {
    if (externalPluginArchive.isPresent) {
        archiveFile.set(rootProject.layout.projectDirectory.file(externalPluginArchive.get()))
    }
}
