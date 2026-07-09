# idea-book-latex-plugin

An IntelliJ IDEA plugin for writing books in LaTeX. It compiles nothing, and it
replaces nothing: the language itself comes from
[TeXiFy IDEA](https://github.com/Hannah-Sten/TeXiFy-IDEA), on which this plugin
depends. On top of it, it does two separate things.

## A page, rather than a screen

The plugin ships a colour scheme named **LaTeX**, and gives it to LaTeX files
only — every other file keeps the scheme you chose for the IDE. It is black ink
on white paper, in a serif face, so that a chapter looks more like the book it
will become than like the source it is.

That face is **EB Garamond**, and it does not come with the plugin. It is free
software, under the SIL Open Font License, and has to be
[installed on the system](https://github.com/octaviopardo/EBGaramond12)
separately. Without it the IDE quietly falls back to something else, which is
not an error and not a disaster: pick another family under *Settings | Editor |
Color Scheme | Font*, or turn the scheme off entirely in *Settings | Tools |
Book LaTeX*.

## Markup that reads as what it means

The other half is the rendering. A novel's markup — comments to yourself, scene
headers, lines of dialogue, foreign phrases — is shown as what it means rather
than as what it is spelled like.

`\sjm{Sono appena arrivato.}` reads as `Jacques: «Sono appena arrivato.»`, and
you can still put the caret in the middle of the sentence and fix a word.

Those commands are not standard LaTeX, and the plugin does not invent them. They
are defined in the preamble of the **Book LaTeX** project:

> <https://github.com/fabiusdieciscudi/book-latex>

`\comment`, `\scene`, `\beat`, `\shot`, the dialogue commands, the foreign
language commands: all of them live there, and the plugin reads that preamble to
learn who speaks. Opened on a LaTeX file that does not use them, it finds nothing
to render and stays out of the way.

Nothing is ever rewritten behind your back except the `%` at the end of a
rendered command, and only when you save. Everything else is a way of *looking*
at the file you already have.

---

## Requirements

- IntelliJ IDEA **2026.1** or later, in the **New UI**
- The **TeXiFy IDEA** plugin (a hard dependency: this plugin will not load without it)
- A manuscript built on the [Book LaTeX](https://github.com/fabiusdieciscudi/book-latex) preamble
- The **EB Garamond** font, if you want the scheme to look as intended

---

## The master switch

Everything this plugin draws can be turned off at once.

The switch lives in the top-right corner of the editor, next to the error
counter, in the same strip where a Markdown file shows its source / split /
preview buttons. Click it and every rendering in this section disappears,
leaving the plain source. Click it again and it comes back.

It is a single setting for the whole IDE, not one per file. It requires the New
UI with editor tabs visible; the platform draws that strip nowhere else.

While the project is being indexed some renderings do not appear yet. They show
up on their own when indexing ends.

---

## Comments

A `\comment{...}` that sits on lines of its own is painted as a yellow sticky
note.

```latex
    \comment{controllare la data di questa scena}%
```

Indentation before the command is fine, and so is a trailing `%`. A `\comment`
in the middle of a sentence is left as source, because the note takes whole
lines.

**Double-click the note** to open an editor for its text: a real LaTeX editor,
with completion, several lines tall. Confirm and the note shows the new text.

A long comment is drawn on one line and cut off on the right; the text is all
still there, in the file.

---

## Scenes, beats and shots

Three commands carry a list of attributes and are drawn as a coloured strip the
width of the editor.

| Command | Attributes | Strip |
| --- | --- | --- |
| `\scene[...]` | name, setting, date, time | dark blue |
| `\beat[...]` | name, driver, pov | light blue |
| `\shot[...]` | focus, pov, sense, framing | lighter blue |

```latex
\scene[
    name={\Jacques commenta con zio \Bernard},
    setting={\settingMevouillonFattoriaCucina},
    date={\sceneToday},
    time={mattina tarda}
]%
```

reads as a strip: **NAME** `\Jacques commenta con zio \Bernard`  **SETTING** … and
so on, left to right.

**Double-click the strip** to open a dialog with one field per attribute, each a
LaTeX editor with completion. What you see is the raw source: the `date` field
holds `\sceneToday`, not a date.

On confirmation the whole `[...]` group is rewritten:

- an attribute you filled in appears, even if it was not there before;
- an attribute you emptied disappears from the source;
- an attribute the plugin does not know about is kept, moved after the others;
- if nothing is left, the `[...]` group is removed entirely.

The command is reformatted one attribute per line, indented, whatever it looked
like before.

An attribute with no value shows a dash on the strip.

Except `date` and `time`. A scene that does not say when it happens happens when
the last one did: the strip shows the value of the most recent earlier `\scene`
that gave it, in a dimmer shade, and the two are looked up separately. The value
is only ever shown. Open the dialog and the field is empty, with the inherited
value greyed behind it as a reminder of what you are about to override;
confirming an untouched field writes nothing. If no earlier scene gave one, the
dash stays.

Like comments, these are only rendered when they own their lines.

---

## Dialogue

The book defines one command per speaker, in the preamble:

```latex
\newcommand{\sjm}[2][]{\spoken{\Jacques}{#2}{#1}}%
\newcommand{\tjm}[2][]{\thought{\Jacques}{#2}{#1}}%
```

The plugin reads those definitions, follows `\Jacques` to its own
`\newcommand`, and strips the typesetting around it. So `\sjm{Sono appena
arrivato.}` is shown as:

> Jacques: «Sono appena arrivato.»

A `\thought` command gets curly quotes instead of guillemets:

> Jacques: “Non ci credo.”

**The sentence stays real text.** Only `\sjm{` and the closing brace are hidden.
The caret goes into the line, the spellchecker sees it, completion works. What
is hidden cannot be un-hidden by clicking: to see the command, turn the master
switch off.

### Typing a speaker

Type `«` in a LaTeX file and a list of every speaker the book defines appears.
Start typing a name to narrow it. Choose one and the guillemet becomes
`\sjm{}`, with the caret between the braces — whereupon the rendering puts the
guillemet back, this time with a name in front of it.

Choose *Keep « as text*, or press Escape, and the character stays as you typed
it. Inside a `%` comment the list does not appear at all.

### When the preamble changes

The speakers are read once and kept, because they change far less often than
the prose. After editing the file that defines them, run:

**Tools | Reload LaTeX Dialogue Definitions**

Anything the plugin could not resolve is written to the IDE log, so a speaker
that shows up with a backslash in its name has an explanation there.

---

## Foreign languages

Two commands per language: one for prose, one for names.

```latex
\french{du pain}        →  FR{du pain}     (in italics)
\frenchname{Jean}       →  FR{Jean}        (upright)
```

Only the command name is hidden. The braces and their contents stay in the
document, so a foreign phrase is edited like any other text.

Thirteen languages are recognised, each shown by its ISO code: Catalan `CA`,
Dutch `NL`, English `EN`, French `FR`, German `DE`, Irish `GA`, Japanese `JA`,
Latin `LA`, Ligurian `LIJ`, Provençal `OC`, Spanish `ES`, Swedish `SV`, Turkish
`TR`.

Ligurian and Provençal have no two-letter ISO code; the three-letter code and
the code of Occitan are used respectively.

---

## Quotations and ellipses

`\sq{testo}` is shown as `“testo”`. The quotes replace the command and its
braces; the text between them is yours to edit.

`\ellipsis` is shown as `…`.

**Type three dots** and they become `\ellipsis`, which is immediately drawn as a
single ellipsis character. Pasted text is left alone, and so is anything after a
`%` on the same line.

---

## Saving

When you save a LaTeX file, the plugin appends the `%` that swallows the newline
to every `\comment`, `\scene`, `\beat` and `\shot` that owns its lines and does
not already end in one. Trailing spaces before it are removed.

A command with real text after it on the same line is never touched, and neither
is a line that already ends in a comment.

This is the only thing the plugin writes to your files without being asked.
Commit before the first save on an existing manuscript, and read the diff.

---

## Settings

**Settings | Tools | Book LaTeX**

- **Use a dedicated colour scheme in LaTeX editors.** LaTeX files are shown with
  a colour scheme of their own while every other file keeps the one you chose in
  the IDE. Pick the scheme from the list; it shows the schemes that exist. Turn
  the option off and the LaTeX editors go back to the global scheme.

  The plugin ships a scheme called **LaTeX**, and selects it by default. It is
  read-only, like the schemes the IDE ships: change any colour in it and the IDE
  saves your version under a new name, leaving the original as something to go
  back to. Pick your copy from the list above and the plugin will use it.

- **Font of rendered blocks.** The family used to paint the sticky notes and the
  attribute strips. Sans-serif by default, so a rendering is not mistaken for the
  prose behind it. Its size follows the editor's, so the blocks grow and shrink
  with `Ctrl`+wheel.

Two things worth knowing about colours. The placeholders — `Jacques:`, the
guillemets, `FR`, `…` — are drawn by the platform with the **Folded text**
attributes of your scheme, under *Settings | Editor | Color Scheme | General |
Code*. Change them there, not here. The sticky note and the strips have colours
of their own, the same under every theme, since a sticky note is yellow
everywhere.

The dialogs open a third of the main window wide the first time. Resize one and
it will reopen where you left it.

---

## Building

The `Makefile` wraps Gradle. Its goals:

| Goal | What it does |
| --- | --- |
| `make` | the same as `make build` |
| `make build` | `./gradlew build` — compiles and runs the tests |
| `make test` | `./gradlew test` — the tests alone, in a second |
| `make run` | builds, then `./gradlew runIde` — launches a sandbox IDE with the plugin installed |
| `make plugin` | `./gradlew buildPlugin` — writes the installable zip under `build/distributions/` |
| `make verify` | `./gradlew verifyPlugin` — runs JetBrains' Plugin Verifier |
| `make clean` | deletes everything a fresh clone would not have |

Use `make run` while developing: the sandbox has its own settings and its own
plugins, and cannot disturb the IDE you are working in. Use `make plugin` to
produce something to install through *Settings | Plugins | Install Plugin from
Disk*.

### Cleaning

`make clean` removes what the build produces: the compiled output, the caches
Gradle keeps inside the project, and the sandbox IDE. Tracked files are never
touched, so the copyright profile and the code style survive it.

Losing the sandbox is the point, not a side effect: its settings, the plugins
installed into it and the indexes it built all go, and the next `make run`
starts from an IDE that has never seen this plugin. It costs a minute.

It does not touch `~/.gradle/caches/`. That directory is shared with every other
Gradle project on the machine, and holds a gigabyte the build would have to
fetch again.

`rm` is used rather than `./gradlew clean`, so that it works when the build is
too broken to run — which tends to be when it is needed.

### Testing

`make test` runs the unit tests. They cover the layer that turns LaTeX into the
text you see — resolving a speaker's name through its macros, deciding whether a
command owns the lines it sits on, rewriting an attribute group — and nothing
else. No IDE is started, so they take about a second.

Everything below that layer is the platform's behaviour, and is checked by
running `make run` and looking.

### Verifying

`make verify` runs the same Plugin Verifier the JetBrains Marketplace runs
before publishing. It downloads the target IDEs and checks the compiled plugin
against their bytecode, without executing anything. It reports:

- classes, methods and fields the plugin refers to that do not exist in that
  IDE, or whose signature has changed;
- use of API marked internal or experimental;
- use of API that is deprecated, or already removed in a later release;
- mistakes in `plugin.xml`: the id, the since/until range, undeclared
  dependencies, extension points that do not exist.

Because it checks against several IDEs, it will also tell you that the plugin
compiles against the version you build with but would break on the next one.

The first run downloads those IDEs, which takes a while. The report is written
to `build/reports/pluginVerifier/`.

### First time

You need a **JDK 21**. The Gradle wrapper is committed, so `./gradlew` — and
the `Makefile` goals that wrap it — work from a fresh clone; the wrapper pins
the version of Gradle the build expects.

The build downloads an entire IntelliJ IDEA distribution to compile against,
which is over a gigabyte. To use the one already installed instead, put its path
in `~/.gradle/gradle.properties` — not in the repository, since it is yours
alone:

```properties
localIdePath=/Applications/IntelliJ IDEA.app
```

Without that property the build falls back to downloading, so a fresh clone and
a continuous integration server both work.

TeXiFy IDEA is fetched from the Marketplace automatically; it is declared in
`build.gradle.kts`.

---

## License

MIT — see [LICENSE](LICENSE).
