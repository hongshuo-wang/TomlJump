plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
}

val pluginDescription = """
    <p>
        TomlJump adds focused navigation from TOML configuration to the files and source declarations
        those settings reference.
    </p>
    <ul>
        <li>Jump from TOML string file paths to project files.</li>
        <li>Jump from TOML tables and keys to matching source declarations.</li>
        <li>Resolve conservative config declarations in Go, Python, Java, TypeScript, and JavaScript.</li>
        <li>Use standard JetBrains navigation actions such as Go to Declaration and command/control click.</li>
    </ul>
    <p>
        TomlJump intentionally does not replace TOML syntax highlighting, formatting, schema validation,
        or completion. It focuses on high-confidence navigation and stays quiet when a TOML value or key
        is not a credible reference.
    </p>
    <hr/>
    <p>
        TomlJump 为 TOML 配置提供专注的跳转能力，可以从配置快速跳到对应的项目文件和源码声明。
    </p>
    <ul>
        <li>从 TOML 字符串文件路径跳转到项目文件。</li>
        <li>从 TOML table 和 key 跳转到匹配的源码配置声明。</li>
        <li>支持 Go、Python、Java、TypeScript、JavaScript 中的保守配置匹配。</li>
        <li>使用 JetBrains 原生跳转能力，例如 Go to Declaration 和 command/control click。</li>
    </ul>
    <p>
        TomlJump 不替代 TOML 语法高亮、格式化、schema 校验或补全。它只专注于高置信度导航；
        当 TOML 值或 key 不是可信引用时会静默跳过。
    </p>
""".trimIndent()

val pluginChangeNotes = """
    <ul>
        <li>更新 JetBrains read-action API 用法，提升与当前 IntelliJ Platform 的兼容性。</li>
        <li>新增 Linux DO 社区讨论入口。</li>
    </ul>
    <hr/>
    <ul>
        <li>Updated JetBrains read-action API usage for compatibility with the current IntelliJ Platform.</li>
        <li>Added a Linux DO community discussion link.</li>
    </ul>
""".trimIndent()

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
}
