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
        <li>新增源码到 TOML 的反向导航，支持从 Go、Python、Java、TypeScript 和 JavaScript 配置容器及字段跳转到匹配的 TOML table 和 key。</li>
        <li>唯一可信目标直接打开；多个可信目标使用 JetBrains 原生选择器，并显示完整 TOML 路径和来源文件。</li>
        <li>加强基于所属配置容器的保守匹配，过滤局部变量、嵌套属性、注释和字符串中的伪声明；关系不明确时保持静默。</li>
    </ul>
    <hr/>
    <ul>
        <li>Added source-to-TOML navigation from configuration containers and fields in Go, Python, Java, TypeScript, and JavaScript to matching TOML tables and keys.</li>
        <li>Unique high-confidence targets open directly; multiple credible targets use the native JetBrains chooser with full TOML paths and source file locations.</li>
        <li>Strengthened conservative, owner-aware matching to reject false declarations in local variables, nested properties, comments, and strings while staying quiet for uncertain relationships.</li>
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
