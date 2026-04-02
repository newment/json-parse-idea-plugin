# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a JetBrains IntelliJ IDEA plugin (`ink.ican.json-parse`) that provides a JSON formatting tool with multiple utilities. The plugin is built with Gradle using the `intellij-platform` plugin.

## Build Commands

```bash
./gradlew build          # Build the plugin JAR
./gradlew runIde         # Run the plugin in a development IDE
./gradlew jar            # Create just the plugin JAR
./gradlew publishPlugin  # Publish to JetBrains marketplace
```

Requires Java 21 (configured in `build.gradle.kts`).

## Architecture

### Two Tool Window Approaches

The plugin has two `ToolWindowFactory` implementations:

1. **JsonFormatToolWindowFactory** (`src/main/java/.../ui/`) - Swing-based UI using `JBTextArea` input and `JTextPane` with syntax highlighting via regex-based coloring. Uses Jackson's `ObjectMapper` for JSON formatting.

2. **JsonFormatHtmlWindowFactory** (`src/main/java/.../ui/`) - JCEF-based UI that loads HTML resources. Extracts packaged HTML files from the JAR to a temp directory at runtime.

### HTML Resources

HTML resources (`src/main/resources/*.html`) are bundled in the JAR and extracted to a temp directory by `JsonFormatHtmlWindowFactory.getOrExtractResources()`. The `index.html` acts as a shell that loads individual tool pages (`json-format.html`, `base64.html`, etc.) into an iframe.

### Plugin Configuration

- **Plugin ID**: `ink.ican.json-parse`
- **Tool Window ID**: `JsonParse` (registered in `plugin.xml`)
- **Minimum IDE Version**: 251 (2024.1+)
- **Action**: `JsonWindowOpenAction` adds a "Json-Parse" entry to the Tools menu

### Key Files

- `src/main/resources/META-INF/plugin.xml` - Plugin descriptor
- `src/main/java/ink/ican/jsonparse/ui/JsonFormatHtmlWindowFactory.java` - Main UI factory
- `src/main/java/ink/ican/jsonparse/ui/JsonFormatToolWindowFactory.java` - Swing-based formatter (legacy)
- `src/main/resources/index.html` - Main HTML shell

### Dependencies

- **Jackson** (`com.fasterxml.jackson`) - JSON parsing and formatting
- **Gson** (`com.google.code.gson`) - Also available for JSON processing
- **JBCef** (`com.intellij.ui.jcef`) - Chromium-based embedded browser for HTML UI