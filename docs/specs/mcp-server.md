# MCP server in Studio

Status: **implemented**

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
* A server is permitted if its full name is selected, or **any of its ancestor folders** is selected
  (direct or indirect parent, up to the root `/`). Therefore servers added into a selected folder (or any of
  its sub-folders) after the permission was given are permitted automatically.
* Example: `/a/b` is selected. `/a/b/Server` and `/a/b/x/y/Other` are permitted. If `/a/b/Server` is moved to
  `/a/c/Server`, it is no longer permitted unless `/a/c`, `/a` or `/` is selected.
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

`edit_tab` modes: only `text` replaces the whole text; `find` + `text` replaces the single exact occurrence of
`find`; `start` (+ optional `end`) + `text` replaces a character range or inserts at `start`.

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

## AI agent queries window

Background (`server`) and ad-hoc (`connection`) queries can be shown in a separate Studio window,
titled `[AI agent queries] ...`. Queries in `tab` mode are not shown there, since they already appear in the user's tab.

* Opened automatically on the first background query when the setting *Show queries which agents run on
  background connections in a separate window* is on (default on), without taking the focus;
  or on demand from the status bar widget (*Show agent queries window*), showing queries from then on.
* One editor tab per server. Every query is appended with a header comment, e.g.
  `/ 2026.11.18 14:02:31 claude-code: confirmed by the user`. Denied queries are added as comments
  (`denied by the user (not executed)` / `not confirmed in time`).
* While a query runs, its tab shows the *Executing* status with the clock, and the toolbar *Stop* cancels it
  (the background connection is closed). A cancelled query gets the comment `/ ^ cancelled by the user`.
* Every executed query adds a result tab with the full result (errors as error tabs); the usual result tab
  limit applies. The query is not executed again.
* The tabs are normal editors: the user can edit and run them (through the user's own connection).
  Agents see them in `list_tabs` (flag `agentQueriesWindow`).
* The window is not saved in the workspace and is not restored after restart. Closing the last regular
  window exits Studio even if the agent window is open.

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
* Active grants are visible from the status bar widget and can be revoked there (see *Grants window*).
  All grants are revoked when the MCP server is stopped or settings change.
* The dialog shows a countdown (`Auto-deny in 4:37`); when it reaches zero the query is denied
  (confirmation timeout, default 5 min) and the dialog closes.

### Grants window

Opened from the status bar widget popup (*Active grants...*), and also via a marker on the widget while
any grant is active. A non-modal window listing the time-limited grants:

| Server | Granted at | Expires | Remaining | Queries run | |
|---|---|---|---|---|---|
| /Dev/hdb1 | 14:02 | 14:17 | 9:41 | 3 | [Revoke] |
| hdb2.dev:5010 (ad-hoc) | 13:50 | until exit | - | 12 | [Revoke] |

* Remaining time updates live; expired grants disappear.
* *Revoke* removes a single grant; *Revoke all* removes every grant. Revocations are logged.

## Running queries and cancellation

All agent queries in progress (tab and background modes) are tracked. The status bar widget shows
`• N running` and its popup lists them (`Cancel: <query> (<server>, <seconds> s)`) with *Cancel all agent queries*.
Cancelling a tab query works like the Stop button of the tab; cancelling a background query closes its
connection. The agent receives "The query was cancelled by the user".

## Status bar widget

Shown in every window's status bar while MCP is enabled:

* `MCP :7878` (grey) - listening, no agent connected.
* `MCP: claude-code` (green) - an agent session is active (initialized and a request within the last 5 minutes).
  Several agents: `MCP: 2 agents`.
* Badge/marker when time-limited grants are active.
* `MCP: error` (red) - failed to start (e.g. port busy); tooltip shows the reason.
* Click - popup: connected agents (name, version, address, last activity), running queries with *Cancel*, *Active grants...* (opens the
  Grants window), *Revoke all grants*, *Open MCP log*, *Settings...*.

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

## Code map

* `studio.mcp.McpServer` - HTTP transport and JSON-RPC; `McpSessions` - connected agents; `McpTool` - tool model.
* `studio.mcp.StudioTools` - tool implementations (Swing access on the EDT), query confirmation and background connections.
* `studio.mcp.McpPermissions` - permitted servers/folders; `McpGrants` - time-limited grants.
* `studio.mcp.McpService` - lifecycle (applies settings), permissions cleanup on server tree changes.
* `studio.mcp.ResultRenderer` - text/JSON rendering of results.
* `studio.mcp.ui` - confirmation dialog, grants window, connect-agent dialog, permission tree, status bar widget.
* `studio.ui.settings.SettingsMcpTab` - the settings tab.
* Tests: `test/studio/mcp/*` (unit), `test-integration/studio/ui/McpTest` (GUI, with a mocked kdb connection).

## Connecting agents

The *Connect an agent...* dialog has a client selector and shows a generated snippet (actual URL, port and
token filled in) with a *Copy* button. Templates live in one class so they're easy to update as clients change:

* **Claude Code** - `claude mcp add --transport http kdbstudio http://127.0.0.1:7878/mcp --header "Authorization: Bearer <token>"`
* **GitHub Copilot** - one entry per Copilot surface (agent mode is required to use MCP tools; for Copilot
  Business/Enterprise the organization policy *MCP servers in Copilot* must be enabled):
  * *VS Code* - workspace `.vscode/mcp.json` or user configuration (command *MCP: Add Server...* / *MCP: Open User
    Configuration*): `{"servers": {"kdbstudio": {"type": "http", "url": "...", "headers": {...}}}}`
  * *JetBrains IDEs* (Copilot plugin) - Copilot Chat, agent mode, *Configure tools* / *Edit config*: `mcp.json`
    with the same `{"servers": {...}}` shape
  * *Visual Studio* - `.mcp.json` in the solution folder or `%USERPROFILE%\.mcp.json`, same `{"servers": {...}}` shape
  * *Copilot CLI* - `/mcp add` interactively, or `~/.copilot/mcp-config.json`:
    `{"mcpServers": {"kdbstudio": {"type": "http", "url": "...", "headers": {...}, "tools": ["*"]}}}`
  * Not supported: Copilot coding agent on github.com (runs in the cloud and can't reach Studio on the user's machine)
* **Cursor** - `~/.cursor/mcp.json`: `{"mcpServers": {"kdbstudio": {"url": "...", "headers": {...}}}}`
* **Gemini CLI** - `settings.json`: `{"mcpServers": {"kdbstudio": {"httpUrl": "...", "headers": {...}}}}`
* **Codex CLI** - `~/.codex/config.toml`: `[mcp_servers.kdbstudio]` with `url` and the bearer token setting
* **Generic / stdio-only clients** (e.g. Claude Desktop) - via the `mcp-remote` bridge:
  `{"mcpServers": {"kdbstudio": {"command": "npx", "args": ["mcp-remote", "http://127.0.0.1:7878/mcp", "--header", "Authorization: Bearer <token>"]}}}`

Templates are in `studio.mcp.AgentConfigTemplates`. Also covered: Windsurf (`serverUrl`).
The formats were written from the clients' documentation as known at the time of implementation and should be
re-checked when a client changes its configuration format.

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
