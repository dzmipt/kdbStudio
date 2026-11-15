# MCP server in Studio

Status: **draft for review**

## Goal

Let AI agents (Claude Code, Cursor, VS Code Copilot, Codex, Gemini CLI, ...) work with a running Studio
through the [Model Context Protocol](https://modelcontextprotocol.io): read and edit editor tabs, open new
tabs, read results, and run queries on servers the user has explicitly permitted, with every query
confirmed by the user in Studio.

## Architecture

* Embedded HTTP server based on the JDK `com.sun.net.httpserver.HttpServer`; JSON via the existing Gson
  dependency. No new third-party dependencies.
* MCP "Streamable HTTP" transport on a single endpoint `http://<host>:<port>/mcp`:
  * `POST` - JSON-RPC requests / notifications, plain `application/json` responses (no SSE streaming needed).
  * `GET` - `405 Method Not Allowed` (no server-initiated messages).
  * `DELETE` - ends the session.
  * Session id issued on `initialize` in the `Mcp-Session-Id` header.
  * `Origin` header is validated (rejects browser pages from other origins - DNS rebinding protection).
* Methods: `initialize`, `notifications/initialized`, `ping`, `tools/list`, `tools/call`.
* New package `studio.mcp`:
  * `McpServer` - lifecycle (start/stop/restart on settings change), HTTP handling, auth, sessions.
  * `JsonRpc` - request/response/error model.
  * `McpTools` - tool registry: name, description, JSON input schema, handler.
  * `McpPermissions` - allowed servers/folders, ad-hoc policy, time-limited grants.
  * `QueryConfirmationDialog`.
  * `McpStatusWidget` - status bar widget.
* Tool handlers run on HTTP worker threads; all Swing access goes through `SwingUtilities.invokeAndWait`.
* Start/stop happens immediately when settings are applied (no restart). If the port can't be bound, an
  error is shown and logged; Studio keeps working.

## Settings

New Settings tab **"AI Agent (MCP)"**, stored in `studio.json`:

| Setting | Default | Notes |
|---|---|---|
| Enable MCP server | off | |
| Port | 7878 | Any free port 1024-65535 |
| Accept connections from | This machine only | `This machine only` binds to `127.0.0.1`; `Any machine` binds to `0.0.0.0` |
| Access token | `Required for remote connections` | `Not required` / `Required for remote connections` / `Required for all connections`. Token is generated automatically; buttons *Regenerate*, *Copy*. Sent as `Authorization: Bearer <token>` |
| Allowed servers | none | Tree with checkboxes / multi-selection and a filter, similar to Bulk Edit (see below) |
| Allow ad-hoc connections | off | Agent may connect to any `host:port` providing user/password and auth method |
| Max rows returned | 100 | Hard cap; agent can request fewer |
| Max characters returned | 20000 | Hard cap for any text returned by tools |
| Confirmation timeout | 5 min | Unanswered confirmation dialog = denied |
| *Connect an agent...* button | | Opens a dialog with ready-to-paste configuration for various clients (see below) |

### Allowed servers

* The user selects servers and/or folders. Stored as a list of paths: folder paths (e.g. `/Prod/EU`) and
  server full names (e.g. `/Dev/hdb1`).
* A server is permitted if its full name is selected, or **any of its ancestor folders** is selected.
  Therefore new servers added into a selected folder (or any of its sub-folders) are permitted automatically.
* Editing a server's connection details, auth method, colour etc. keeps the permission (the full name is
  unchanged). Renaming or moving a server/folder changes its path and drops the permission (by design).
* Cleanup: whenever the server tree changes (`ServerConfig.setRoot`), stored paths that no longer exist in
  the tree are removed and the settings saved.

## Tools

Tab and result ids are short strings (e.g. `t3`, `r12`) assigned by Studio, valid while Studio runs.

| Tool | Input | Returns |
|---|---|---|
| `list_servers` | optional `filter` | Allowed servers only: full name, folder, host, port, TLS, auth method. **Never user/password.** Also whether ad-hoc connections are allowed |
| `list_tabs` | - | All editor tabs in all windows: id, window, title, file name, server, modified flag, active flag, running flag |
| `get_tab` | `tab` | Text, selection (start/end/text), caret position, server |
| `edit_tab` | `tab`, one of: `text` (replace all) / `range`+`text` (replace range) / `position`+`text` (insert) | New length; edit goes into the editor undo history as a single undoable edit |
| `new_tab` | optional `text`, optional `server` (must be allowed), optional `window` | New tab id |
| `set_tab_server` | `tab`, `server` (allowed) or ad-hoc connection | - |
| `execute_query` | `query`; and either `tab` **or** `server` **or** ad-hoc `connection`; optional `format` (`text`/`json`), `maxRows` | Result (see below) |
| `list_results` | optional `window` | Result tabs: id, window, title, server, query, execution time, type, row count, error flag |
| `get_result` | `result`, optional `format`, `maxRows` | Result |

Not available to agents: loading files from disk, saving tabs, closing tabs, closing windows.

### execute_query modes

* `tab` - runs exactly as if the user pressed Execute in that tab, on the tab's current server (which must be
  permitted). The result appears in Studio's result pane and is also returned to the agent. Error if the tab is
  already running a query.
* `server` - runs on a background connection (one cached connection per server, not tied to any editor tab).
  Nothing is shown in the Studio UI except the confirmation dialog and the status widget.
* `connection` (ad-hoc, only if allowed) - `{host, port, user, password, authMethod, tls}`; background
  connection, like `server`.

The HTTP request blocks until the user answers the confirmation and the query completes.
The existing incoming message size limit (`kdbMessageSizeLimitMB`) applies.

### Result format

* `text` (default) - console-style rendering as in Studio's console view, truncated to max rows / characters,
  with a note like `... 100 of 25,341 rows shown`.
* `json` - `{type, columns: [{name, type}], rows: [[...]], totalRows, truncated}` for tables / keyed tables /
  dictionaries; `{type, value}` for atoms and lists (value rendered as q text).
* Errors - `isError: true` with the kdb error text (e.g. `'type`).

## Query confirmation

Every `execute_query` pops up a modal dialog (the Studio window is brought to front):

```
 AI agent "claude-code" wants to run a query

 Server:  /Dev/hdb1  (hdb1.dev:5010)            Tab: t3 "analysis.q"  | background
 ┌──────────────────────────────────────────────────────────────────┐
 │ select count i by sym from trade where date=.z.d-1              │
 └──────────────────────────────────────────────────────────────────┘

 [ Run ]  [ Deny ]          [ Allow for: (15 minutes ▾) ]
```

* **Run** - this query only. **Deny** - the agent receives "denied by user".
* **Allow for** - runs this query and auto-approves further queries **on the same server** (ad-hoc: same
  `host:port`) for the selected interval: 5 min / 15 min / 1 hour / 4 hours / until Studio exits.
  The last chosen interval is remembered.
* Active grants are visible in the status bar widget and can be revoked there. All grants are revoked
  when the MCP server is stopped or settings change.
* Unanswered for the confirmation timeout = denied.

## Status bar widget

Shown in every window's status bar while MCP is enabled:

* `MCP :7878` (grey) - listening, no agent connected.
* `MCP: claude-code` (green) - an agent session is active (initialized and a request within the last 5 minutes).
  Several agents: `MCP: 2 agents`.
* Badge/marker when time-limited grants are active.
* `MCP: error` (red) - failed to start (e.g. port busy); tooltip shows the reason.
* Click - popup: connected agents (name, version, address, last activity), active grants with *Revoke*,
  *Open MCP log*, *Settings...*.

## Activity log

Separate file `mcp.log` next to `query.log` (logger `MCP`, rolling 10 MB x 10). Logged:

* server start/stop, bind address and port, start failures;
* agent connect/disconnect: client name/version, remote address; authentication failures; rejected origins;
* every tool call: tool, arguments (passwords masked), outcome, duration;
* queries: server, mode (tab/background/ad-hoc), full query text, confirmation outcome
  (`run` / `denied` / `timeout` / `auto-approved by grant`), result type and row count or error;
* edits: tab, operation, range, length of inserted/removed text;
* permission violations (non-permitted server, ad-hoc disabled).

Queries run by agents are also written into the existing `query.log` as usual.

## Connecting agents

The *Connect an agent...* dialog has a client selector and shows a generated snippet (actual URL, port and
token filled in) with a *Copy* button. Templates live in one class so they're easy to update as clients change:

* **Claude Code** - `claude mcp add --transport http kdbstudio http://127.0.0.1:7878/mcp --header "Authorization: Bearer <token>"`
* **VS Code (Copilot)** - `.vscode/mcp.json`: `{"servers": {"kdbstudio": {"type": "http", "url": "...", "headers": {...}}}}`
* **Cursor** - `~/.cursor/mcp.json`: `{"mcpServers": {"kdbstudio": {"url": "...", "headers": {...}}}}`
* **Gemini CLI** - `settings.json`: `{"mcpServers": {"kdbstudio": {"httpUrl": "...", "headers": {...}}}}`
* **Codex CLI** - `~/.codex/config.toml`: `[mcp_servers.kdbstudio]` with `url` and the bearer token setting
* **Generic / stdio-only clients** (e.g. Claude Desktop) - via the `mcp-remote` bridge:
  `{"mcpServers": {"kdbstudio": {"command": "npx", "args": ["mcp-remote", "http://127.0.0.1:7878/mcp", "--header", "Authorization: Bearer <token>"]}}}`

Exact formats are to be verified against each client's current documentation during implementation.

## Security notes

* Off by default; localhost-only by default.
* Credentials of configured servers are never exposed; Studio authenticates on the agent's behalf.
* Agent-supplied ad-hoc passwords are never logged.
* `Origin` validation against browser-based attacks on localhost.
* Plain HTTP only (no TLS) - for remote access, recommend requiring a token and using it on trusted networks
  or via an SSH tunnel.

## Implementation steps

1. Config keys + `McpPermissions` (path matching, cleanup on server tree change) + unit tests.
2. JSON-RPC / MCP HTTP transport, auth, origin check, sessions + unit tests with a real HTTP client.
3. Tools: servers, tabs, edits, new tab; result rendering (text/json).
4. Query execution (tab / background / ad-hoc), confirmation dialog, grants.
5. Settings tab, *Connect an agent* dialog.
6. Status bar widget, `mcp.log`.
7. Docs (`notes.md`), manual end-to-end test with MCP Inspector and Claude Code.
