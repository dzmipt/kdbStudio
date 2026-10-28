# Style schemas

Status: implemented.

## Goal

Let the user switch the editor and result grid colours as a whole ("style schema"),
with predefined Light and Dark schemas, user-defined schemas, and a remembered
choice per Look and Feel (L&F).

## What a schema contains

A schema holds everything the Style tab edits today except the L&F and fonts:

| Part           | Today's studio.json key | Content                                         |
|----------------|-------------------------|-------------------------------------------------|
| `editorColors` | `editorColors`          | background, selected, current line highlight    |
| `tokens`       | `tokens`                | colour and font style (plain/bold/italic) per syntax token |
| `gridColors`   | `gridColors`            | foreground/background for null, key, even, odd, marked, selected, marked and selected |

Out of scope (stay global / unchanged):

- L&F and fonts (editor and result table).
- Chart colours (`chartColorSets`).
- Server background colour rules (`serverBgColorRules`) keep their explicit colours
  and know nothing about light or dark schemas.

The UI calls the feature "Style schema", to avoid confusion with the chart "Color schema".

## Predefined schemas

- Shipped in the jar as `resources/styleSchemas.json`, containing `Light` and `Dark`.
  The format is the same as a user schema in studio.json (see below), so duplicating
  is a plain copy.
- Schemas from the plugin folder's default `studio.json` (`styleSchemas` key) are also
  treated as predefined.
- Predefined schemas are read-only: their colour editors are disabled, Rename and Delete
  are disabled. To change one the user clicks Duplicate.
- Names of predefined schemas are reserved; a user schema can't use them.
- A unit test checks that every schema in `resources/styleSchemas.json` defines every
  editor colour, token and grid colour.
- The Dark colours shipped initially are placeholders, to be replaced with the final palette.
- The current Java defaults (`ColorToken` defaults, `ColorMap.DEFAULT_EDITOR_COLORS`,
  grid colours derived from the L&F) are no longer used, except by the migration,
  which needs them to rebuild the user's old look exactly.

## User schemas

- Created only with **Duplicate…** from any schema (predefined or user).
  - Tooltip: says it creates a new schema from the selected one.
  - Asks for the name (suggests e.g. "Dark copy"). Name must be non-empty and unique,
    and not a predefined name.
  - The new schema stores `basedOn`: the name of the predefined schema it ultimately
    comes from (duplicating a user schema copies its `basedOn`).
- Editable, can be renamed and deleted.
- **Rename** changes the name and updates every L&F mapped to the old name.
- **Delete** removes the schema; L&Fs mapped to it go back to the default rule below.
- Stored with full values. If a later release adds a new token or colour that a user
  schema doesn't have, the value is taken from its `basedOn` schema
  (or `Light` if `basedOn` is missing or unknown).

## Which schema is active

- Every L&F (by class name) can have a remembered schema.
- The active schema = the remembered schema for the current L&F; if none (or it no
  longer exists): `Dark` for a FlatLaf dark theme (`isDark()`), otherwise `Light`.
  Non-FlatLaf L&Fs (Metal, Nimbus, Windows, GTK, …) count as light.
- There is no light/dark marker on user schemas. Any schema can be chosen for any L&F.

## Style tab behaviour

```
Look and Feel: [FlatLaf Dark   ▼]
Style schema:  [Dark           ▼] [Duplicate…] [Rename…] [Delete]
──────────
Editor font …                      (global)
Colors for tokens:   …             (disabled for predefined schemas)
Preview
──────────
Result table font …                (global)
Colors for result grid: …          (disabled for predefined schemas)
```

- Changing the L&F in the dialog immediately switches the schema combo to the
  schema that L&F would use (remembered or default rule); previews update.
- Choosing a schema in the combo updates the previews.
- Colour edits apply to the selected user schema. Switching between schemas keeps
  unsaved edits in memory.
- The existing "Reset to default" buttons are removed (predefined schemas can't be
  changed, so there is nothing to reset to).
- Choosing a schema in the combo remembers it for the L&F currently selected in the dialog.
- **OK** (or **Apply**) saves all schema changes and the remembered schemas.
- **Cancel** restores everything to how it was before the dialog was opened: edits,
  new, renamed and deleted schemas, and L&F mappings.

## Server background colour

- A server's background colour becomes optional: "Default" means "use the active
  schema's editor background".
- The server editor and the multiple-server editor get a "Default" option.
- New servers are created with "Default".
- In servers.json, a server with "Default" has no `bgColor`.
- The dialog "Background color is changed. Do you want to update background for all
  servers with old background color?" is removed.

## studio.json format

```json
"styleSchemas": {
  "Custom": {
    "basedOn": "Light",
    "editorColors": { … },
    "tokens": { … },
    "gridColors": { … }
  },
  "My dark": { "basedOn": "Dark", … }
},
"lookAndFeelSchemas": {
  "com.formdev.flatlaf.FlatDarculaLaf": "My dark"
}
```

- `styleSchemas` is an object keyed by schema name (not an array), so it merges with
  the plugin-folder default studio.json per name.
- `editorColors`, `tokens` and `gridColors` use the same JSON format as today's
  top-level keys. An empty `gridColors` object (`{}`) means grid colours derived from the
  L&F (the behaviour before 2.4); only a migrated `Custom` schema uses it.
- There is no `selectedSchema` key; the active schema follows from `lookandfeel` and
  `lookAndFeelSchemas`.
- The top-level `editorColors`, `tokens` and `gridColors` keys are removed.

## Migration (config version 2.4)

Runs in `ConfigConverter` (`convertTo24`) when the user's studio.json version is below 2.4.

1. Back up studio.json and servers.json with the existing
   `FilesBackup.getInstance().backup(path)` (copies into the `backup` folder) before
   rewriting them. No `*.bak` files.
2. If the user's studio.json has any of `editorColors`, `tokens`, `gridColors`:
   - create user schema `Custom` with the user's values; missing parts are filled
     from the old Java defaults so the look doesn't change (grid colours that were never
     customised stay derived from the L&F);
   - values equal to the old defaults don't count as changes;
   - `basedOn`: `Light`;
   - map the current `lookandfeel` to `Custom` in `lookAndFeelSchemas` (only the
     current L&F);
   - remove the old top-level keys.
3. If none of those keys are present: nothing to convert; the default rule applies
   (Light or Dark depending on the L&F).
4. servers.json (has no version field; it is converted as part of the studio.json 2.4
   step): servers whose `bgColor` equals the old editor background (the user's
   `editorColors.background`, or white if not set) become "Default". Other servers keep
   their colour.
5. Old keys (`editorColors`, `tokens`, `gridColors`) found in the plugin folder's default
   studio.json are ignored with a warning in the log.
