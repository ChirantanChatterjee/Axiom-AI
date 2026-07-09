import { useEffect, useMemo, useRef, useState } from "react";
import axios from "axios";
import { AnimatePresence, motion } from "framer-motion";
import {
  FiArrowLeft, FiBarChart2, FiBookOpen, FiDatabase, FiExternalLink,
  FiHelpCircle, FiLock, FiLogOut, FiMail, FiMessageSquare, FiMoon, FiPlus,
  FiSend, FiShield, FiSun, FiTrash2, FiUpload, FiUser
} from "react-icons/fi";
import { isSupabaseConfigured, supabase } from "./supabaseClient";
import "./index.css";

const AUTH_STORAGE_KEY = "aif.auth.session.v1";
const CHAT_STORAGE_PREFIX = "aif.chat.sessions.v2";
const ACTIVE_CHAT_PREFIX = "aif.chat.activeSession.v2";
const CHAT_THEME_STORAGE_KEY = "aif.chat.theme.v1";
const TERMINAL_EXECUTION_STATUSES = new Set(["PASSED", "FAILED", "CANCELLED"]);
const DEFAULT_API_BASE_URL = "[axiom-ai-production-1ab3.up.railway.app](https://axiom-ai-production-1ab3.up.railway.app)";
const LOCAL_BACKEND_HOSTS = new Set(["localhost", "127.0.0.1", "0.0.0.0"]);
const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE_URL;

const isLocalBrowserHost = () => {
  if (typeof window === "undefined") return true;
  return LOCAL_BACKEND_HOSTS.has(window.location.hostname);
};

const isLocalBackendUrl = (value) => {
  try { return LOCAL_BACKEND_HOSTS.has(new URL(value).hostname); }
  catch { return false; }
};

const API_BASE_URL =
    !isLocalBrowserHost() && isLocalBackendUrl(configuredApiBaseUrl)
        ? DEFAULT_API_BASE_URL
        : configuredApiBaseUrl;

const welcomeMessage = { sender: "ai", text: "AIF Runtime Intelligence Ready.", type: "info" };

const createChatId = () => {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
  return `chat-${Date.now()}-${Math.random().toString(16).slice(2)}`;
};

const createChatSession = () => {
  const now = new Date().toISOString();
  return {
    id: createChatId(), title: "New chat", websiteUrl: null, domainName: null,
    frameworkLocked: false, createdAt: now, updatedAt: now,
    messages: [{ ...welcomeMessage }]
  };
};

const userStorageKey = (user) =>
    user?.email?.trim().toLowerCase().replace(/[^a-z0-9._-]+/g, "_") || "guest";

const chatStorageKey = (user) => `${CHAT_STORAGE_PREFIX}.${userStorageKey(user)}`;
const activeChatKey  = (user) => `${ACTIVE_CHAT_PREFIX}.${userStorageKey(user)}`;

const loadStoredAuth = () => {
  try {
    const stored = localStorage.getItem(AUTH_STORAGE_KEY);
    return stored ? JSON.parse(stored) : null;
  } catch { return null; }
};

const loadStoredChatTheme = () => {
  try {
    return localStorage.getItem(CHAT_THEME_STORAGE_KEY) === "dark" ? "dark" : "light";
  } catch { return "light"; }
};

const sessionTokenForUser = (user) =>
    user?.sessionToken || user?.token || user?.sessionId || user?.authToken || "";

const loadChats = (user) => {
  try {
    const stored = localStorage.getItem(chatStorageKey(user));
    if (!stored) return [createChatSession()];
    const parsed = JSON.parse(stored);
    if (Array.isArray(parsed) && parsed.length > 0) return parsed;
  } catch {
    // Fall through to a fresh session when stored chat data is invalid.
  }
  return [createChatSession()];
};

const loadInitialChatState = (user) => {
  const chats = loadChats(user);
  const storedActiveId = localStorage.getItem(activeChatKey(user));
  const activeChatId = chats.some(chat => chat.id === storedActiveId)
      ? storedActiveId : chats[0].id;
  return { chats, activeChatId };
};

const normalizeChatSession = (chat) => {
  const fallback = createChatSession();
  if (!chat) return fallback;
  return {
    ...fallback, ...chat,
    id: chat.id || chat.sessionId || fallback.id,
    title: chat.title || "New chat",
    websiteUrl: chat.websiteUrl || null,
    domainName: chat.domainName || null,
    frameworkLocked: Boolean(chat.frameworkLocked),
    createdAt: chat.createdAt || fallback.createdAt,
    updatedAt: chat.updatedAt || fallback.updatedAt,
    messages: Array.isArray(chat.messages) && chat.messages.length > 0
        ? chat.messages : fallback.messages
  };
};

const authHeaders = (user) => ({ "X-AIF-Session": sessionTokenForUser(user) });

const reportChatSyncFailure = (action, error) => {
  const status = error?.response?.status;
  const url = error?.config?.url;
  const detail = [status ? `status=${status}` : null, url ? `url=${url}` : null].filter(Boolean).join(" ");
  console.warn(`AIF chat sync ${action} failed${detail ? ` (${detail})` : ""}.`, error);
};

const saveRemoteChatSession = async (user, chat) => {
  if (!sessionTokenForUser(user) || !chat?.id) return;
  await axios.put(
      `${API_BASE_URL}/api/workspace/sessions/${encodeURIComponent(chat.id)}`,
      normalizeChatSession(chat),
      { headers: authHeaders(user) }
  );
};

const isWelcomeOnlyChat = (chat) => {
  const messages = Array.isArray(chat?.messages) ? chat.messages : [];
  return messages.length === 1 &&
      messages[0]?.sender === welcomeMessage.sender &&
      messages[0]?.text === welcomeMessage.text;
};

const isMeaningfulChat = (chat) =>
    Boolean(chat?.websiteUrl || chat?.domainName || chat?.frameworkLocked ||
        (chat?.title && chat.title !== "New chat") || !isWelcomeOnlyChat(chat));

const chatTime = (chat) => {
  const value = Date.parse(chat?.updatedAt || chat?.createdAt || "");
  return Number.isNaN(value) ? 0 : value;
};

const mergeChatSessions = (remoteChats, localChats) => {
  const merged = new Map();
  remoteChats.forEach(chat => {
    const normalized = normalizeChatSession(chat);
    merged.set(normalized.id, normalized);
  });
  localChats.map(normalizeChatSession).filter(isMeaningfulChat).forEach(localChat => {
    const remoteChat = merged.get(localChat.id);
    if (!remoteChat || chatTime(localChat) >= chatTime(remoteChat)) {
      merged.set(localChat.id, {
        ...remoteChat, ...localChat,
        messages: Array.isArray(localChat.messages) && localChat.messages.length > 0
            ? localChat.messages : remoteChat?.messages
      });
    }
  });
  return Array.from(merged.values()).sort((l, r) => chatTime(r) - chatTime(l));
};

const loadRemoteChatState = async (user) => {
  if (!sessionTokenForUser(user)) return loadInitialChatState(user);
  const localState = loadInitialChatState(user);
  const guestState = userStorageKey(user) === "guest"
      ? { chats: [], activeChatId: null } : loadInitialChatState(null);
  const localChats = mergeChatSessions(localState.chats, guestState.chats);
  try {
    const response = await axios.get(`${API_BASE_URL}/api/workspace/sessions`, { headers: authHeaders(user) });
    const remoteChats = Array.isArray(response.data) ? response.data.map(normalizeChatSession) : [];
    if (remoteChats.length > 0) {
      const mergedChats = mergeChatSessions(remoteChats, localChats);
      const storedActiveId = localStorage.getItem(activeChatKey(user)) ||
          localState.activeChatId || guestState.activeChatId;
      localStorage.setItem(chatStorageKey(user), JSON.stringify(mergedChats));
      await Promise.all(mergedChats.filter(isMeaningfulChat).map(chat =>
          saveRemoteChatSession(user, chat).catch(error => { reportChatSyncFailure("save", error); return null; })
      ));
      return {
        chats: mergedChats,
        activeChatId: mergedChats.some(chat => chat.id === storedActiveId) ? storedActiveId : mergedChats[0].id
      };
    }
  } catch (error) {
    reportChatSyncFailure("load", error);
    return loadInitialChatState(user);
  }
  await Promise.all(localChats.map(chat =>
      saveRemoteChatSession(user, chat).catch(error => { reportChatSyncFailure("save", error); return null; })
  ));
  localStorage.setItem(chatStorageKey(user), JSON.stringify(localChats));
  return {
    chats: localChats,
    activeChatId: localChats.some(chat => chat.id === localState.activeChatId)
        ? localState.activeChatId : localChats[0]?.id
  };
};

const titleFromMessage = (text) => {
  const cleaned = text.trim().replace(/\s+/g, " ");
  if (cleaned.length <= 38) return cleaned;
  return `${cleaned.slice(0, 35)}...`;
};

const formatChatTime = (value) => {
  if (!value) return "";
  try {
    return new Intl.DateTimeFormat(undefined, { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" })
        .format(new Date(value));
  } catch { return ""; }
};

const domainFromUrl = (url) => {
  if (!url) return null;
  try { return new URL(url).hostname.replace(/^www\./, ""); }
  catch { return url.replace("https://","").replace("http://","").replace(/^www\./,"").split("/")[0]; }
};

const compactResponseData = (type, data) => {
  if (!data) return null;
  if (Array.isArray(data)) return data;
  if (type === "framework") return { sessionId: data.sessionId, frameworkPath: data.frameworkPath, downloadUrl: data.downloadUrl, flowsDetected: data.flowsDetected, flows: data.flows || [], testCaseCount: data.testCaseCount, testCases: data.testCases || [], websiteUrl: data.websiteUrl, domainName: data.domainName };
  if (type === "feature") return { featureName: data.featureName, frameworkPath: data.frameworkPath, downloadUrl: data.downloadUrl, variables: data.variables, testCaseCount: data.testCaseCount, testCases: data.testCases };
  if (type === "generated-tests") return { featureName: data.featureName || "generated tests", frameworkPath: data.frameworkPath, tags: data.tags || [], testCaseCount: data.testCaseCount, testCases: data.testCases || [] };
  if (type === "generated-test-execution") return { success: data.success, tagExpression: data.tagExpression, reportUrl: data.reportUrl, exitCode: data.exitCode };
  if (type === "generated-test-execution-queued") return { jobId: data.jobId, status: data.status, success: data.success, tagExpression: data.tagExpression, reportUrl: data.reportUrl, exitCode: data.exitCode, message: data.message, errorMessage: data.errorMessage };
  if (type === "download") return { sessionId: data.sessionId, downloadUrl: data.downloadUrl };
  if (type === "framework-upload") return { sessionId: data.sessionId, frameworkRoot: data.frameworkRoot, fileName: data.fileName, tags: data.tags || [], learningSummary: data.learningSummary };
  if (type === "missing-variables") return { missingVariables: data.missingVariables || [], variableDetails: data.variableDetails || [], example: data.example || "" };
  return data;
};

const isSensitiveKey = (key) => {
  const lower = key.toLowerCase();
  return lower.includes("password") || lower.includes("token") || lower.includes("secret") || lower.includes("otp");
};

const runtimeVariablesForMessage = (data) => {
  const variables = [];
  const addVariable = (value) => {
    if (typeof value !== "string" || !value.trim()) return;
    const normalized = value.trim();
    if (!variables.includes(normalized)) variables.push(normalized);
  };
  (data?.missingVariables || []).forEach(addVariable);
  (data?.variableDetails || []).forEach(detail => addVariable(detail?.variable));
  return variables;
};

const emptyRuntimeValueMap = (variables) =>
    Object.fromEntries(variables.map(variable => [variable, ""]));

const runtimeDetailsForVariable = (data, variable) =>
    (data?.variableDetails || []).filter(detail => detail?.variable === variable);

const savedVariableSummary = (variables) =>
    Object.fromEntries(Object.keys(variables || {}).map(variable => [variable, "Saved"]));

const isHandledStructuredType = (type) =>
    ["tags","generated-tests","generated-test-execution","generated-test-execution-queued","framework","feature",
      "download","framework-upload","missing-variables","session_guard","variables"].includes(type);

const messageContentClassName = (msg) => {
  const classes = ["message-content"];
  classes.push(isHandledStructuredType(msg?.type) || msg?.type === "error" ? "message-important" : "message-plain");
  if (msg?.type) classes.push(`message-type-${msg.type}`);
  return classes.join(" ");
};

const normalizeBackendUrl = (url) => {
  if (!url) return null;
  if (url.startsWith("http://") || url.startsWith("https://")) {
    try {
      const parsedUrl = new URL(url);
      if (parsedUrl.pathname.startsWith("/api/") && LOCAL_BACKEND_HOSTS.has(parsedUrl.hostname)) {
        const apiBaseUrl = new URL(API_BASE_URL);
        return `${apiBaseUrl.origin}${parsedUrl.pathname}${parsedUrl.search}${parsedUrl.hash}`;
      }
    } catch { return url; }
    return url;
  }
  if (url.startsWith("reports/")) return `${API_BASE_URL}/api/reports/${url.split("/").pop()}`;
  if (url.startsWith("/")) return `${API_BASE_URL}${url}`;
  return `${API_BASE_URL}/${url}`;
};

const filenameFromContentDisposition = (value) => {
  if (!value) return "aif-framework.zip";
  const match = value.match(/filename="?([^";]+)"?/i);
  return match?.[1] || "aif-framework.zip";
};

const oauthErrorFromLocation = () => {
  if (typeof window === "undefined") return "";
  const currentUrl = new URL(window.location.href);
  const hashParams = new URLSearchParams(currentUrl.hash.startsWith("#") ? currentUrl.hash.slice(1) : currentUrl.hash);
  return currentUrl.searchParams.get("error_description") || hashParams.get("error_description") ||
      currentUrl.searchParams.get("error") || hashParams.get("error") || "";
};

const oauthRedirectUrl = () =>
    typeof window === "undefined" ? "/auth/callback" : `${window.location.origin}/auth/callback`;

const isOAuthCallbackPath = () =>
    typeof window !== "undefined" && window.location.pathname === "/auth/callback";

const clearOAuthRoute = () => {
  if (typeof window === "undefined") return;
  const nextPath = isOAuthCallbackPath() ? "/" : window.location.pathname;
  window.history.replaceState({}, document.title, `${window.location.origin}${nextPath}`);
};

const providerLabel = (provider) =>
    provider === "google" ? "Google" : provider === "azure" ? "Microsoft" : "Social";

const formatOAuthError = (provider, message) => {
  const label = providerLabel(provider);
  const detail = message || "OAuth provider returned an error.";
  const lower = detail.toLowerCase();
  if (lower.includes("unsupported provider") || lower.includes("provider is not enabled"))
    return `${label} sign in is not enabled in Supabase Auth. Enable the ${label} provider in Supabase, then add ${oauthRedirectUrl()} as an allowed redirect URL.`;
  if (lower.includes("redirect") || lower.includes("callback"))
    return `${label} sign in could not complete because the redirect URL is not allowed. Add ${oauthRedirectUrl()} in Supabase Auth URL configuration.`;
  return `${label} sign in failed: ${detail}`;
};

const formatAdminDate = (value) => {
  if (!value) return "Not available";
  try { return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)); }
  catch { return value; }
};

const adminValue = (value) => {
  if (value === null || value === undefined || value === "") return "Not available";
  return String(value);
};

// ─── Binary rain data ────────────────────────────────────────────────────────

const BINARY_CHARS = [
  "0","1","0","1","1","0","0","1","1","0",
  "1","0","1","0","0","1","0","1","1","0",
  "1","1","0","0","1","0","1","0","1","1",
  "0","1","0","1","0","0","1","1","0","1",
];

const COL_2_CHARS = [
  "A","I","F","R","U","N","T","I","M","E",
  "0","1","E","X","E","C","0","1","P","A",
  "S","S","F","A","I","L","R","U","N","0",
  "1","T","A","G","S","0","1","A","I","F",
];

const COL_3_CHARS = [
  "1","0","1","1","0","1","0","0","1","1",
  "0","1","0","1","1","0","1","0","0","1",
  "1","0","0","1","0","1","1","0","1","0",
  "1","1","0","0","1","0","1","1","0","1",
];

const COL_4_CHARS = [
  "F","R","A","M","E","W","O","R","K","0",
  "1","G","E","N","0","1","R","E","P","A",
  "I","R","0","1","T","E","S","T","0","1",
  "L","O","C","K","0","1","A","U","T","O",
];

// ─── Binary Rain Component ───────────────────────────────────────────────────

function BinaryRain() {
  return (
      <div className="binary-rain">
        {/* Column 1 — binary, warm orange */}
        <div className="binary-rain-col">
          {BINARY_CHARS.map((ch, i) => (
              <span key={i} style={{ animationDelay: `${i * 0.09}s`, animationDuration: `${1.8 + (i % 5) * 0.3}s` }}>
            {ch}
          </span>
          ))}
        </div>

        {/* Column 2 — AIF keywords, white */}
        <div className="binary-rain-col">
          {COL_2_CHARS.map((ch, i) => (
              <span key={i} style={{ animationDelay: `${i * 0.07}s`, animationDuration: `${1.5 + (i % 4) * 0.35}s` }}>
            {ch}
          </span>
          ))}
        </div>

        {/* Column 3 — binary, orange */}
        <div className="binary-rain-col">
          {COL_3_CHARS.map((ch, i) => (
              <span key={i} style={{ animationDelay: `${i * 0.11}s`, animationDuration: `${2.0 + (i % 6) * 0.25}s` }}>
            {ch}
          </span>
          ))}
        </div>

        {/* Column 4 — AIF keywords, purple */}
        <div className="binary-rain-col">
          {COL_4_CHARS.map((ch, i) => (
              <span key={i} style={{ animationDelay: `${i * 0.08}s`, animationDuration: `${1.6 + (i % 5) * 0.28}s` }}>
            {ch}
          </span>
          ))}
        </div>

        {/* Centre badge overlay */}
        <div className="binary-rain-label">
          <div className="binary-rain-badge">AIF</div>
          <span className="binary-rain-tag">runtime · intelligence</span>
        </div>
      </div>
  );
}

const authFlowNodes = [
  { label: "GENERATE", x: 7, y: 18, cx: 14, cy: 5, color: "#5ed7ff" },
  { label: "MEMORY", x: 25, y: 14, cx: 35, cy: 17, color: "#f1b211" },
  { label: "EXECUTE", x: 45, y: 22, cx: 54, cy: 6, color: "#7dffb2" },
  { label: "REPAIR", x: 66, y: 15, cx: 78, cy: 10, color: "#b89cff" },
  { label: "REPORT", x: 86, y: 27, cx: 96, cy: 42, color: "#ff74bb" },
  { label: "FLOW", x: 74, y: 47, cx: 66, cy: 58, color: "#52f5df" },
  { label: "TAGS", x: 53, y: 42, cx: 43, cy: 50, color: "#ff9a62" },
  { label: "RUN", x: 34, y: 55, cx: 22, cy: 55, color: "#6fa8ff" },
  { label: "PASS", x: 12, y: 68, cx: 11, cy: 85, color: "#cffe5e" },
  { label: "LOCK", x: 31, y: 84, cx: 45, cy: 95, color: "#ff6c55" },
  { label: "AIF", x: 59, y: 78, cx: 75, cy: 70, color: "#D94126" },
  { label: "READY", x: 88, y: 86, cx: 110, cy: 22, color: "#ffffff" }
];

const authFlowStepSeconds = 1.45;

function AuthFlowBackdrop() {
  return (
      <div className="auth-flow-backdrop" aria-hidden="true">
        <svg
            className="auth-flow-svg"
            viewBox="0 0 100 100"
            preserveAspectRatio="none"
            focusable="false"
        >
          <defs>
            {authFlowNodes.map((node, index) => (
                <marker
                    id={`auth-flow-arrow-${index}`}
                    key={`auth-flow-arrow-${index}`}
                    markerWidth="7"
                    markerHeight="7"
                    refX="6"
                    refY="3.5"
                    orient="auto"
                >
                  <path d="M 0 0 L 7 3.5 L 0 7 z" fill={node.color} fillOpacity="0.42" />
                </marker>
            ))}
          </defs>

          {authFlowNodes.map((node, index) => {
            const nextNode =
                authFlowNodes[(index + 1) % authFlowNodes.length];

            return (
                <path
                    className="auth-flow-line"
                    key={`auth-flow-line-${index}`}
                    d={`M ${node.x} ${node.y} Q ${node.cx} ${node.cy} ${nextNode.x} ${nextNode.y}`}
                    markerEnd={`url(#auth-flow-arrow-${index})`}
                    style={{
                      "--flow-color": node.color,
                      "--flow-delay": `${(index * authFlowStepSeconds) + (authFlowStepSeconds * 0.34)}s`
                    }}
                />
            );
          })}
        </svg>

        {authFlowNodes.map((node, index) => (
            <span
                className="auth-flow-node"
                data-label={node.label}
                key={node.label}
                style={{
                  "--flow-x": `${node.x}%`,
                  "--flow-y": `${node.y}%`,
                  "--flow-color": node.color,
                  "--flow-delay": `${index * authFlowStepSeconds}s`
                }}
            >
              <span>{node.label}</span>
            </span>
        ))}
      </div>
  );
}

// ─── Header bar wordmark ─────────────────────────────────────────────────────

function AuthHeaderBar() {
  const word1 = "AIF";
  const word2 = "Runtime";

  return (
      <div className="auth-header-bar">
        <div style={{ display: "flex", alignItems: "center", justifyContent: "center" }}>
          <div className="auth-header-emblem">AIF</div>

          <div className="auth-wordmark" aria-label="AIF Runtime">
            {/* "AIF" — A is accent colour */}
            {word1.split("").map((letter, i) => (
                <span
                    key={`w1-${i}`}
                    className={
                      i === 0
                          ? "auth-wordmark-letter auth-wordmark-letter--accent"
                          : "auth-wordmark-letter"
                    }
                    style={{ color: i === 0 ? "#D94126" : "#ffffff" }}
                >
              {letter}
            </span>
            ))}

            <span className="auth-wordmark-space" />

            {/* "Runtime" — all black */}
            {word2.split("").map((letter, i) => (
                <span
                    key={`w2-${i}`}
                    className="auth-wordmark-letter"
                    style={{ color: "#ffffff" }}
                >
              {letter}
            </span>
            ))}
          </div>
        </div>

        <p className="auth-header-subtitle">Agent Infrastructure Foundation</p>
      </div>
  );
}

// ─── Runtime Vault Visual ────────────────────────────────────────────────────

function RuntimeVaultVisual() {
  return (
      <div className="runtime-visual" aria-hidden="true">
        <div className="runtime-grid"></div>
        <div className="runtime-orbit runtime-orbit-outer"></div>
        <div className="runtime-orbit runtime-orbit-inner"></div>
        <div className="runtime-line runtime-line-one"></div>
        <div className="runtime-line runtime-line-two"></div>
        <div className="runtime-line runtime-line-three"></div>
        <div className="runtime-core">
          <span>AIF</span>
          <small>Runtime Core</small>
        </div>
        <div className="runtime-chip runtime-chip-generate"><span>GENERATE</span><strong>Framework</strong></div>
        <div className="runtime-chip runtime-chip-tags"><span>TAGS</span><strong>@auto</strong></div>
        <div className="runtime-chip runtime-chip-run"><span>RUN</span><strong>Queued</strong></div>
        <div className="runtime-chip runtime-chip-report"><span>REPORT</span><strong>Ready</strong></div>
        <div className="runtime-chip runtime-chip-pass"><span>PASS</span><strong>Stable</strong></div>
        <div className="runtime-code-chip runtime-code-left">locator.selfHeal()</div>
        <div className="runtime-code-chip runtime-code-right">repair.prompt++</div>
      </div>
  );
}

// ─── Login success overlay ───────────────────────────────────────────────────

function LoginSuccessOverlay({ onDone }) {
  useEffect(() => {
    const timer = window.setTimeout(onDone, 1400);
    return () => window.clearTimeout(timer);
  }, [onDone]);

  return (
      <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.25 }}
          style={{
            position: "fixed", inset: 0, zIndex: 9999, display: "flex",
            alignItems: "center", justifyContent: "center",
            background: "#080810", flexDirection: "column", gap: 20, pointerEvents: "none"
          }}
      >
        {[0, 1, 2].map(i => (
            <motion.div
                key={i}
                initial={{ scale: 0.4, opacity: 0.8 }}
                animate={{ scale: 3.5 + i * 1.2, opacity: 0 }}
                transition={{ duration: 1.1, delay: i * 0.12, ease: [0.2, 0.9, 0.3, 1] }}
                style={{
                  position: "absolute", width: 180, height: 180, borderRadius: "50%",
                  border: `${2 - i * 0.5}px solid rgba(227, 114, 94, ${0.7 - i * 0.2})`,
                  pointerEvents: "none"
                }}
            />
        ))}
        <motion.div
            initial={{ scale: 0.5, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ type: "spring", stiffness: 260, damping: 18, delay: 0.1 }}
            style={{
              position: "relative", width: 90, height: 90, borderRadius: 22,
              background: "linear-gradient(135deg, #e3725e 0%, #c4522e 100%)",
              display: "flex", alignItems: "center", justifyContent: "center",
              fontSize: 18, fontWeight: 900, color: "#ffffff", letterSpacing: "-0.04em",
              boxShadow: "0 0 60px rgba(227, 114, 94, 0.7), 0 24px 56px rgba(0,0,0,0.5)"
            }}
        >
          AIF
        </motion.div>
        <motion.p
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.35, duration: 0.4 }}
            style={{
              position: "relative", color: "rgba(255,255,255,0.72)", fontSize: 14,
              fontWeight: 700, letterSpacing: "0.08em", textTransform: "uppercase",
              fontFamily: "Inter, sans-serif"
            }}
        >
          Runtime Intelligence Ready
        </motion.p>
      </motion.div>
  );
}

// ─── Auth Screen ─────────────────────────────────────────────────────────────

function AuthScreen({ mode, form, loading, error, onModeChange, onFormChange, onPresetDomain, onSubmit }) {
  return (
      <div className="auth-page">
        {/* Ambient orbs */}
        <div aria-hidden="true" style={{ position:"absolute", width:520, height:520, borderRadius:"50%", background:"radial-gradient(circle, rgba(227,114,94,0.20) 0%, transparent 70%)", top:"-10%", left:"-8%", animation:"authOrbDrift 18s ease-in-out infinite", pointerEvents:"none", zIndex:0 }} />
        <div aria-hidden="true" style={{ position:"absolute", width:440, height:440, borderRadius:"50%", background:"radial-gradient(circle, rgba(124,110,227,0.16) 0%, transparent 70%)", bottom:"5%", right:"-6%", animation:"authOrbDrift2 22s ease-in-out infinite", pointerEvents:"none", zIndex:0 }} />
        <div aria-hidden="true" style={{ position:"absolute", width:300, height:300, borderRadius:"50%", background:"radial-gradient(circle, rgba(94,207,167,0.12) 0%, transparent 70%)", bottom:"30%", left:"40%", animation:"authOrbDrift3 26s ease-in-out infinite", pointerEvents:"none", zIndex:0 }} />

        <AuthFlowBackdrop />

        {/* ── Rich header wordmark at top centre ── */}
        <AuthHeaderBar />

        {/* ── 3-column stage ── */}
        <div className="login-stage">
          {/* Left: brand info + visual */}
          <motion.section
              className="auth-brand"
              initial={{ opacity: 0, x: -22 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ type: "spring", stiffness: 90, damping: 18 }}
          >
            <p>
              Generate frameworks, execute flows, repair failures, and preserve session-scoped automation intelligence in one runtime.
            </p>

            <RuntimeVaultVisual />

            <div className="auth-capabilities">
              <div><FiShield />Session-scoped framework memory</div>
              <div><FiMessageSquare />Chat-driven test execution</div>
              <div><FiBookOpen />AI repair and report intelligence</div>
            </div>
          </motion.section>

          {/* Centre: login panel */}
          <motion.section
              className="auth-panel"
              initial={{ opacity: 0, y: 22, scale: 0.97 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              transition={{ type: "spring", stiffness: 110, damping: 20, delay: 0.1 }}
          >
            <div className="auth-tabs">
              <motion.button type="button" className={mode === "login" ? "active" : ""} onClick={() => onModeChange("login")} whileHover={{ y: -1 }} whileTap={{ scale: 0.98 }}>
                Log in
              </motion.button>
              <motion.button type="button" className={mode === "signup" ? "active" : ""} onClick={() => onModeChange("signup")} whileHover={{ y: -1 }} whileTap={{ scale: 0.98 }}>
                Sign up
              </motion.button>
            </div>

            <form className="auth-form" onSubmit={onSubmit}>
              <div className="auth-form-heading">
                <h2>{mode === "login" ? "Welcome back" : "Create your workspace"}</h2>
                <p>{mode === "login" ? "Use your email and password to open your chat sessions." : "Use an email address and password to create your workspace."}</p>
              </div>

              {error && <div className="auth-error">{error}</div>}

              {mode === "signup" && (
                  <label className="field-label">
                    <span>Name</span>
                    <div className="field-control">
                      <FiUser />
                      <input value={form.displayName} onChange={e => onFormChange({ displayName: e.target.value })} placeholder="Your name" autoComplete="name" />
                    </div>
                  </label>
              )}

              <label className="field-label">
                <span>Email</span>
                <div className="field-control">
                  <FiMail />
                  <input value={form.email} onChange={e => onFormChange({ email: e.target.value })} placeholder="name@example.com" type="email" autoComplete="email" required />
                </div>
              </label>

              <div className="email-presets">
                <button type="button" onClick={() => onPresetDomain("company.com")}>Work email</button>
              </div>

              <label className="field-label">
                <span>Password</span>
                <div className="field-control">
                  <FiLock />
                  <input value={form.password} onChange={e => onFormChange({ password: e.target.value })} placeholder="Minimum 8 characters" type="password" autoComplete={mode === "login" ? "current-password" : "new-password"} required />
                </div>
              </label>

              <motion.button type="submit" className="auth-submit" disabled={loading} whileHover={loading ? undefined : { y: -2 }} whileTap={loading ? undefined : { scale: 0.985 }}>
                {loading ? "Working..." : mode === "login" ? "Log in" : "Create account"}
              </motion.button>
            </form>
          </motion.section>

          {/* Right: binary rain */}
          <motion.section
              className="auth-scenery"
              initial={{ opacity: 0, x: 22 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ type: "spring", stiffness: 90, damping: 18, delay: 0.15 }}
          >
            <BinaryRain />
          </motion.section>
        </div>
      </div>
  );
}

// ─── Help View ───────────────────────────────────────────────────────────────

function HelpView({ onBack }) {
  return (
      <div className="help-page">
        <div className="help-header">
          <div><span>AIF Knowledge Article</span><h2>Command Helper</h2></div>
          <button type="button" className="secondary-action" onClick={onBack}><FiArrowLeft />Chat with AIF</button>
        </div>
        <div className="help-grid">
          <section className="help-section"><h3>Generate A Framework</h3><p>Ask AIF to create an automation framework for a website. One chat should stay tied to one website and one framework.</p><code>Generate framework for [saucedemo.com](https://www.saucedemo.com)</code><code>Can you generate framework for [parabank.parasoft.com](https://parabank.parasoft.com/parabank/admin.htm)</code></section>
          <section className="help-section"><h3>Create Or Extend Tests</h3><p>Create generated Cucumber tests after the framework exists, or add more coverage to an existing feature.</p><code>Can you generate tests for bill pay?</code><code>Can you create tests for register a user?</code><code>Add negative and boundary tests for checkout.</code><code>Create tests for registration using valid, missing-field, invalid-email, and duplicate-user scenarios.</code></section>
          <section className="help-section"><h3>Compound Requests</h3><p>You can combine generation and execution in one message. Say which feature should be generated and which generated-test tag should run.</p><code>Create tests for register a user, then run generated tests with tag @bill_pay.</code><code>Generate tests for bill pay, then list the generated tags.</code><code>Create tests for login and then run all generated tests.</code></section>
          <section className="help-section"><h3>Find Generated Tags</h3><p>Ask for available tags before execution. AIF responds with the generated tags and what each one covers.</p><code>Can you please provide me with the tags of the generated tests?</code></section>
          <section className="help-section"><h3>Run Tests</h3><p>Execute one tag, multiple tags, or the complete generated suite. The chat response includes the Cucumber report link.</p><code>Can you please run the tests with tag @bill_pay?</code><code>Can you run tests with @ai_requirement?</code><code>Run tests matching (@bill_pay or @billpay).</code><code>Can you please run all the generated tests?</code><code>Run generated Cucumber tests for register a user.</code></section>
          <section className="help-section"><h3>Run Flow Vs Generated Tests</h3><p>Use "generated tests" or a tag when you want Cucumber execution. Use "flow" only for detected workspace flows.</p><code>Run generated tests with tag @register.</code><code>Run the LOGIN flow.</code><code>Run tests for register a user using the generated Cucumber framework.</code></section>
          <section className="help-section"><h3>Repair Failed Generated Tests</h3><p>Ask AIF to inspect the latest generated-test run. It checks the last Cucumber output and only changes generated files when it can identify a safe repair.</p><code>The last test failed, can you look at it again and fix it?</code><code>I see some failures, can you please fix it?</code><code>Fix the failed generated test from the last report.</code></section>
          <section className="help-section"><h3>Guide A Repair</h3><p>Point AIF at the failing area when you know whether the issue is a locator, assertion, invalid step, wait, navigation, or test data.</p><code>The field locator used for "Email" field is incorrect. Can you please fix it?</code><code>The field locator used for "Email" field should be "input[name='email']". Please update it.</code><code>The assertion is incorrect because the actual expectation should be "Bill Payment Complete".</code><code>The step "Old Button" is invalid. Can you please remove it?</code></section>
          <section className="help-section"><h3>Correct Assertion Text</h3><p>When the page text is different from the generated assertion, provide the actual sentence from the app or report.</p><code>The test failed because the assertion sentence actually was "Please enter a valid number.", can you fix it?</code><code>The actual sentence is --&gt; Please enter a valid number.</code><code>The expected text should be "Bill Payment Complete".</code></section>
          <section className="help-section"><h3>Fix Locators Or Navigation</h3><p>If a report shows an unresolved element or wrong page, describe the missing target and the visible label.</p><code>The field locator used for "Email" field should be "input[name='email']".</code><code>The last test could not find "send payment button"; the button text is "SEND PAYMENT". Please fix it.</code></section>
          <section className="help-section"><h3>Fix Test Data Issues</h3><p>If the report shows login, account, or form-data problems, update the saved runtime values and rerun the same tag.</p><code>The failure was caused by bad credentials. Use username chirantan and password chirantan.</code><code>Update amount to 100.00 and rerun @bill_pay.</code></section>
          <section className="help-section"><h3>Supply Runtime Variables</h3><p>If a generated test needs values, give them in chat. Sensitive values are stored in the session and masked in the UI.</p><code>Use username standard_user and password secret_sauce</code><code>username is chirantan, password is chirantan, account is 00000121, amount is 1000.00</code></section>
          <section className="help-section"><h3>Open Or Explain Reports</h3><p>Ask for the latest report or paste a report link when you want AIF to inspect failures and screenshots.</p><code>Open the latest execution report.</code><code>Can you explain why the last generated test failed?</code></section>
          <section className="help-section"><h3>Download Or Inspect Workspace</h3><p>Ask for framework artifacts, generated reports, or workspace database state from the active session.</p><code>Download the generated framework.</code><code>Show database.</code><code>Show DB records for this workspace.</code></section>
          <section className="help-section"><h3>Upload Modified Frameworks</h3><p>If you edit the downloaded framework locally, upload it back into the same chat before listing tags or running tests.</p><code>Upload modified framework, then list generated tags.</code><code>I uploaded the fixed framework. Run generated tests with tag @bill_pay.</code></section>
          <section className="help-section"><h3>Session Rule</h3><p>When a chat already owns a framework, create a new chat for a different website. This keeps generated files, reports, and test context clean.</p><code>New chat {">"} Generate framework for another website</code></section>
          <section className="help-section"><h3>Requirement Analysis and Test Creation</h3><p>Paste requirements directly in chat when you want AIF to derive test cases and generated scenarios from them.</p><code>Can you create tests from the below requirements? {"-->"} Paste your requirements</code></section>
        </div>
      </div>
  );
}

// ─── Profile View ─────────────────────────────────────────────────────────────

function ProfileView({ user, onBack }) {
  const initials = (user?.displayName || user?.email || "A").slice(0, 1).toUpperCase();
  return (
      <div className="profile-page">
        <div className="help-header">
          <div><span>AIF Profile</span><h2>Account</h2></div>
          <button type="button" className="secondary-action" onClick={onBack}><FiArrowLeft />Chat with AIF</button>
        </div>
        <div className="profile-body">
          <section className="profile-card">
            <div className="profile-avatar large">
              {user?.avatarUrl ? <img src={user.avatarUrl} alt={user.displayName || user.email} /> : initials}
            </div>
            <div><h3>{user?.displayName}</h3><p>{user?.email}</p></div>
          </section>
          <section className="profile-details">
            <div><span>Provider</span><strong>{user?.provider || "email"}</strong></div>
            <div><span>Role</span><strong>{user?.role || "USER"}</strong></div>
            <div><span>Session</span><strong>{sessionTokenForUser(user) ? "Active" : "Not available"}</strong></div>
          </section>
        </div>
      </div>
  );
}

// ─── Admin View ───────────────────────────────────────────────────────────────

function AdminView({ metrics, loading, error, onBack, onRefresh }) {
  const [activeMetric, setActiveMetric] = useState("users");

  const detailSections = [
    { key: "users", label: "Users", value: metrics?.users, hint: "Registered accounts", items: metrics?.userDetails || [], fields: item => [["Email", item.email],["Name", item.displayName],["Role", item.role],["Provider", item.provider],["Created", formatAdminDate(item.createdAt)],["Last login", formatAdminDate(item.lastLoginAt)]] },
    { key: "authSessions", label: "Auth sessions", value: metrics?.authSessions, hint: "Issued login sessions", items: metrics?.authSessionDetails || [], fields: item => [["User", item.userEmail],["Status", item.status],["Created", formatAdminDate(item.createdAt)],["Expires", formatAdminDate(item.expiresAt)],["Session ID", item.id]] },
    { key: "generatedFrameworks", label: "Generated frameworks", value: metrics?.generatedFrameworks, hint: "Runnable framework folders", items: metrics?.generatedFrameworkDetails || [], fields: item => [["Session", item.sessionId],["File", item.name],["Modified", formatAdminDate(item.modifiedAt)],["Path", item.path]] },
    { key: "generatedFeatures", label: "Generated features", value: metrics?.generatedFeatures, hint: "Gherkin files", items: metrics?.generatedFeatureDetails || [], fields: item => [["Session", item.sessionId],["Feature", item.name],["Modified", formatAdminDate(item.modifiedAt)],["Path", item.path]] },
    { key: "executionReports", label: "Execution reports", value: metrics?.executionReports, hint: "HTML report artifacts", items: metrics?.executionReportDetails || [], fields: item => [["Report", item.name],["Modified", formatAdminDate(item.modifiedAt)],["Path", item.path]] },
    { key: "uploadedFrameworks", label: "Uploaded frameworks", value: metrics?.uploadedFrameworks, hint: "User-modified uploads", items: metrics?.uploadedFrameworkDetails || [], fields: item => [["Session", item.sessionId],["Marker", item.name],["Modified", formatAdminDate(item.modifiedAt)],["Path", item.path]] },
  ];

  const selectedSection = detailSections.find(s => s.key === activeMetric) || detailSections[0];

  return (
      <div className="admin-page">
        <div className="help-header">
          <div><span>AIF Admin</span><h2>Usage Metrics</h2></div>
          <div className="header-actions">
            <button type="button" className="secondary-action" onClick={onRefresh} disabled={loading}><FiBarChart2 />Refresh</button>
            <button type="button" className="secondary-action" onClick={onBack}><FiArrowLeft />Chat with AIF</button>
          </div>
        </div>
        <div className="admin-body">
          {error && <div className="auth-error">{error}</div>}
          <div className="metric-grid">
            {detailSections.map(section => (
                <button key={section.key} type="button" className={activeMetric === section.key ? "metric-card active" : "metric-card"} onClick={() => setActiveMetric(section.key)}>
                  <span>{section.label}</span>
                  <strong>{loading ? "..." : section.value ?? 0}</strong>
                  <small>{section.hint}</small>
                </button>
            ))}
          </div>
          <section className="admin-detail-panel">
            <div className="admin-detail-heading">
              <div><span>Details</span><h3>{selectedSection.label}</h3></div>
              <strong>{loading ? "Loading" : `${selectedSection.items.length} records`}</strong>
            </div>
            <div className="admin-detail-list">
              {!loading && selectedSection.items.length === 0 && <div className="admin-empty">No records found for this metric.</div>}
              {loading && <div className="admin-empty">Loading details...</div>}
              {!loading && selectedSection.items.map((item, index) => (
                  <article className="admin-detail-row" key={`${selectedSection.key}-${item.id || item.path || index}`}>
                    {selectedSection.fields(item).map(([label, value]) => (
                        <div key={label}><span>{label}</span><strong>{adminValue(value)}</strong></div>
                    ))}
                  </article>
              ))}
            </div>
          </section>
        </div>
      </div>
  );
}

// ─── Structured Message ───────────────────────────────────────────────────────

function StructuredMessage({ msg, onDownloadFramework, onSubmitRuntimeVariables, submittingRuntimeVariables = false }) {
  const runtimeVariables = useMemo(() => msg.type === "missing-variables" ? runtimeVariablesForMessage(msg.data) : [], [msg.type, msg.data]);
  const [runtimeValues, setRuntimeValues] = useState(() => emptyRuntimeValueMap(runtimeVariables));
  const hasMissingRuntimeValue = runtimeVariables.some(variable => !runtimeValues[variable]?.trim());

  const submitRuntimeValues = (event) => {
    event.preventDefault();
    if (runtimeVariables.length === 0 || hasMissingRuntimeValue || submittingRuntimeVariables) return;
    const submittedVariables = Object.fromEntries(runtimeVariables.map(variable => [variable, runtimeValues[variable] || ""]));
    onSubmitRuntimeVariables?.(submittedVariables);
    setRuntimeValues(emptyRuntimeValueMap(runtimeVariables));
  };

  return (
      <>
        <div className="plain-text">{msg.text}</div>

        {msg.type === "tags" && msg.data?.tags && (
            <div className="tag-container">
              {msg.data.tags.map((tag, i) => (
                  <div key={i} className="tag-card">
                    <div className="tag-pill">{tag.tag}</div>
                    <div className="tag-description">{tag.description}</div>
                  </div>
              ))}
            </div>
        )}

        {msg.type === "framework" && msg.data && (
            <div className="execution-card">
              <div className="db-row"><span>Website:</span><strong>{msg.data.domainName || msg.data.websiteUrl}</strong></div>
              <div className="db-row"><span>Flows detected:</span><strong>{msg.data.flowsDetected}</strong></div>
              {msg.data.testCaseCount > 0 && <div className="db-row"><span>Test cases:</span><strong>{msg.data.testCaseCount}</strong></div>}
              <div className="db-row"><span>Session:</span><strong>{msg.data.sessionId}</strong></div>
              {msg.data.flows?.length > 0 && (
                  <div className="flow-list">
                    {msg.data.flows.map((flow, index) => (
                        <div className="flow-card" key={`${flow.flowType || "flow"}-${index}`}>
                          <strong>{flow.flowType || "Generated flow"}</strong>
                          <span>{flow.pageUrl}</span>
                          {flow.steps?.length > 0 && (
                              <p>{flow.steps.map(step => `${step.action} ${step.target}`).join(" -> ")}</p>
                          )}
                        </div>
                    ))}
                  </div>
              )}
              {msg.data.testCases?.length > 0 && (
                  <div className="testcase-list">
                    <div className="testcase-row testcase-header"><strong>TC ID</strong><span>Story</span><p>Scenario</p><p>Test Data</p><p>Expected</p></div>
                    {msg.data.testCases.map(tc => (
                        <div key={tc.tcId} className="testcase-row">
                          <strong>{tc.tcId}</strong><span>{tc.userStory}</span><p>{tc.scenario}</p><p>{tc.testData}</p><p>{tc.expectedResult}</p>
                        </div>
                    ))}
                  </div>
              )}
            </div>
        )}

        {(msg.type === "feature" || msg.type === "generated-tests") && msg.data && (
            <div className="execution-card">
              <div className="db-row"><span>Feature:</span><strong>{msg.data.featureName}</strong></div>
              <div className="db-row"><span>Framework:</span><strong>{msg.data.frameworkPath}</strong></div>
              {msg.data.testCaseCount > 0 && <div className="db-row"><span>Test cases:</span><strong>{msg.data.testCaseCount}</strong></div>}
              {msg.data.testCases?.length > 0 && (
                  <div className="testcase-list">
                    <div className="testcase-row testcase-header"><strong>TC ID</strong><span>Story</span><p>Scenario</p><p>Test Data</p><p>Expected</p></div>
                    {msg.data.testCases.map(tc => (
                        <div key={tc.tcId} className="testcase-row">
                          <strong>{tc.tcId}</strong><span>{tc.userStory}</span><p>{tc.scenario}</p><p>{tc.testData}</p><p>{tc.expectedResult}</p>
                        </div>
                    ))}
                  </div>
              )}
            </div>
        )}

        {msg.type === "framework-upload" && msg.data && (
            <div className="execution-card">
              <div className="db-row"><span>Uploaded:</span><strong>{msg.data.fileName}</strong></div>
              <div className="db-row"><span>Recognized tags:</span><strong>{msg.data.tags?.length || 0}</strong></div>
              {msg.data.tags?.length > 0 && (
                  <div className="tag-container compact-tags">
                    {msg.data.tags.map(tag => <div key={tag.tag} className="tag-pill">{tag.tag}</div>)}
                  </div>
              )}
            </div>
        )}

        {msg.type === "variables" && msg.data && (
            <div className="variable-container">
              {Object.keys(msg.data).map(key => (
                  <div key={key} className="variable-pill">
                    <span>{key}</span>
                    <strong>{isSensitiveKey(key) ? "Saved" : msg.data[key]}</strong>
                  </div>
              ))}
            </div>
        )}

        {msg.type === "missing-variables" && msg.data && (
            <form className="missing-variable-card runtime-variable-form" onSubmit={submitRuntimeValues}>
              <strong>Runtime values needed</strong>
              <p className="runtime-variable-note">Enter each value directly in this table. AIF submits these as structured runtime data instead of parsing the chat text.</p>
              <div className="runtime-variable-table-wrap">
                <table className="runtime-variable-table">
                  <thead><tr><th>Required variable</th><th>Value</th></tr></thead>
                  <tbody>
                  {runtimeVariables.map(variable => (
                      <tr key={variable}>
                        <td>
                          <div className="runtime-variable-name"><span>{variable}</span><strong>Required</strong></div>
                          {runtimeDetailsForVariable(msg.data, variable).length > 0 && (
                              <div className="variable-context-list">
                                {runtimeDetailsForVariable(msg.data, variable).map((detail, index) => (
                                    <div key={`${detail.variable || "variable"}-${index}`} className="variable-context-row">
                                      {detail.hint && <span>{detail.hint}</span>}
                                      {detail.scenario && <small>Scenario: {detail.scenario}</small>}
                                      {detail.step && <code>{detail.step}</code>}
                                    </div>
                                ))}
                              </div>
                          )}
                        </td>
                        <td>
                          <input className="runtime-variable-input" type={isSensitiveKey(variable) ? "password" : "text"} value={runtimeValues[variable] || ""} onChange={e => setRuntimeValues(prev => ({ ...prev, [variable]: e.target.value }))} placeholder={`Enter ${variable}`} autoComplete={isSensitiveKey(variable) ? "new-password" : "off"} disabled={submittingRuntimeVariables} required />
                        </td>
                      </tr>
                  ))}
                  </tbody>
                </table>
              </div>
              <div className="runtime-variable-actions">
                <button type="submit" className="runtime-variable-submit" disabled={runtimeVariables.length === 0 || hasMissingRuntimeValue || submittingRuntimeVariables}>
                  {submittingRuntimeVariables ? "Saving..." : "Save values and continue"}
                </button>
              </div>
            </form>
        )}

        {msg.type === "generated-test-execution" && msg.data && (
            <div className="execution-card">
              <div className="db-row"><span>Tag filter:</span><strong>{msg.data.tagExpression || "ALL"}</strong></div>
              <div className="db-row"><span>Exit code:</span><strong className={msg.data.success ? "status-pass" : "status-fail"}>{msg.data.exitCode}</strong></div>
            </div>
        )}

        {msg.type === "generated-test-execution-queued" && msg.data && (
            <div className="execution-card">
              <div className="db-row"><span>Status:</span><strong className={msg.data.status === "PASSED" ? "status-pass" : msg.data.status === "FAILED" ? "status-fail" : ""}>{msg.data.status || "QUEUED"}</strong></div>
              <div className="db-row"><span>Tag filter:</span><strong>{msg.data.tagExpression || "ALL"}</strong></div>
              {msg.data.message && <div className="db-row"><span>Message:</span><strong>{msg.data.message}</strong></div>}
              {msg.data.errorMessage && <div className="db-row"><span>Error:</span><strong className="status-fail">{msg.data.errorMessage}</strong></div>}
              {msg.data.exitCode !== null && msg.data.exitCode !== undefined && (
                  <div className="db-row"><span>Exit code:</span><strong className={msg.data.success ? "status-pass" : "status-fail"}>{msg.data.exitCode}</strong></div>
              )}
            </div>
        )}

        {msg.type === "session_guard" && msg.data && (
            <div className="session-lock-card">
              <FiLock />
              <div><span>Current framework</span><strong>{msg.data.currentDomain || msg.data.currentWebsite}</strong></div>
              <div><span>Requested website</span><strong>{msg.data.requestedDomain || msg.data.requestedWebsite}</strong></div>
            </div>
        )}

        {msg.downloadUrl && (
            <button type="button" className="report-link" onClick={() => onDownloadFramework?.(msg.downloadUrl)}>
              <FiExternalLink />Download Framework
            </button>
        )}

        {msg.reportUrl && (
            <a href={normalizeBackendUrl(msg.reportUrl)} target="_blank" rel="noreferrer" className="report-link">
              <FiExternalLink />
              {msg.type === "generated-test-execution" || msg.type === "generated-test-execution-queued" ? "Open Cucumber Report" : "Open Execution Report"}
            </a>
        )}

        {Array.isArray(msg.data) && (
            <div className="db-container">
              <div className="db-title"><FiDatabase />Database Entries</div>
              {msg.data.map((item, i) => (
                  <div key={i} className="db-card">
                    <div className="db-row"><span>Step:</span><strong>{item.stepOrder}</strong></div>
                    <div className="db-row"><span>Action:</span><strong>{item.action}</strong></div>
                    <div className="db-row"><span>Status:</span><strong className={item.status === "PASSED" ? "status-pass" : "status-fail"}>{item.status}</strong></div>
                    <div className="db-row"><span>Element:</span><strong>{item.elementName}</strong></div>
                  </div>
              ))}
            </div>
        )}

        {msg.data && !isHandledStructuredType(msg.type) && !Array.isArray(msg.data) && (
            <pre className="response-box">{JSON.stringify(msg.data, null, 2)}</pre>
        )}
      </>
  );
}

// ─── App ──────────────────────────────────────────────────────────────────────

function App() {
  const [authUser, setAuthUser] = useState(loadStoredAuth);
  const [showLoginSuccess, setShowLoginSuccess] = useState(false);
  const authSessionToken = sessionTokenForUser(authUser);
  const [chatSyncReady, setChatSyncReady] = useState(() => !sessionTokenForUser(loadStoredAuth()));
  const [authMode, setAuthMode] = useState("login");
  const [authForm, setAuthForm] = useState({ email: "", password: "", displayName: "" });
  const [authLoading, setAuthLoading] = useState(false);
  const [socialProvider, setSocialProvider] = useState("");
  const [authError, setAuthError] = useState(() => {
    const oauthError = oauthErrorFromLocation();
    return oauthError ? formatOAuthError("social", oauthError) : "";
  });
  const [view, setView] = useState("chat");
  const [initialChatState] = useState(() => authUser ? loadInitialChatState(authUser) : { chats: [], activeChatId: null });
  const [chats, setChats] = useState(initialChatState.chats);
  const [activeChatId, setActiveChatId] = useState(initialChatState.activeChatId);
  const [input, setInput] = useState("");
  const [loadingChatId, setLoadingChatId] = useState(null);
  const [deletingChatIds, setDeletingChatIds] = useState([]);
  const [uploadingFramework, setUploadingFramework] = useState(false);
  const [adminMetrics, setAdminMetrics] = useState(null);
  const [adminLoading, setAdminLoading] = useState(false);
  const [adminError, setAdminError] = useState("");
  const [chatTheme, setChatTheme] = useState(loadStoredChatTheme);

  const inputRef = useRef(null);
  const frameworkUploadRef = useRef(null);
  const messagesEndRef = useRef(null);
  const remoteLoadedTokenRef = useRef(null);

  const activeChat = useMemo(() => chats.find(chat => chat.id === activeChatId) || chats[0], [chats, activeChatId]);
  const messages = activeChat?.messages || [];
  const isChatDark = chatTheme === "dark";
  const chatThemeToggleLabel = isChatDark ? "Switch to light mode" : "Switch to dark mode";

  useEffect(() => { if (!oauthErrorFromLocation()) return; clearOAuthRoute(); }, []);

  useEffect(() => {
    if (!isSupabaseConfigured || !supabase) return undefined;
    let cancelled = false;

    const applySupabaseSession = async (session) => {
      const supabaseUser = session?.user;
      if (!supabaseUser?.email) return;
      const metadata = supabaseUser.user_metadata || {};
      const response = await axios.post(`${API_BASE_URL}/api/auth/oauth-login`, {
        email: supabaseUser.email,
        displayName: metadata.full_name || metadata.name || supabaseUser.email,
        provider: supabaseUser.app_metadata?.provider || "supabase",
        providerUserId: supabaseUser.id,
        avatarUrl: metadata.avatar_url || metadata.picture || null
      });
      if (cancelled) return;
      const authenticatedUser = response.data;
      const nextState = await loadRemoteChatState(authenticatedUser);
      remoteLoadedTokenRef.current = sessionTokenForUser(authenticatedUser);
      setAuthUser(authenticatedUser);
      setChats(nextState.chats);
      setActiveChatId(nextState.activeChatId);
      setChatSyncReady(true);
      setDeletingChatIds([]);
      setInput("");
      setSocialProvider("");
      setView("chat");
      setShowLoginSuccess(true);
      clearOAuthRoute();
    };

    supabase.auth.getSession().then(({ data }) => {
      if (!data.session) {
        if (isOAuthCallbackPath() && !oauthErrorFromLocation()) {
          setAuthError(`Social sign in returned without a Supabase session. Add ${oauthRedirectUrl()} to Supabase Auth redirect URLs and confirm the provider is enabled.`);
          setSocialProvider("");
        }
        return null;
      }
      return applySupabaseSession(data.session);
    }).catch(error => {
      if (!cancelled) {
        setAuthError(error.response?.data?.message || error.response?.data?.error || error.message || "Unable to restore Supabase session.");
        setSocialProvider("");
      }
    });

    const { data } = supabase.auth.onAuthStateChange((event, session) => {
      if (event === "SIGNED_IN" || event === "TOKEN_REFRESHED") {
        applySupabaseSession(session).catch(error => {
          if (!cancelled) {
            setAuthError(error.response?.data?.message || error.response?.data?.error || error.message || "Unable to complete social sign in.");
            setSocialProvider("");
          }
        });
      }
    });

    return () => { cancelled = true; data.subscription.unsubscribe(); };
  }, []);

  useEffect(() => {
    let cancelled = false;
    const hydrateChatState = async () => {
      if (!authSessionToken) { if (!cancelled) setChatSyncReady(true); return; }
      if (remoteLoadedTokenRef.current === authSessionToken) { if (!cancelled) setChatSyncReady(true); return; }
      if (!cancelled) setChatSyncReady(false);
      try {
        const nextState = await loadRemoteChatState(authUser);
        if (cancelled) return;
        remoteLoadedTokenRef.current = authSessionToken;
        setChats(nextState.chats);
        setActiveChatId(nextState.activeChatId);
        setDeletingChatIds([]);
        setChatSyncReady(true);
      } catch (error) {
        reportChatSyncFailure("hydrate", error);
        if (!cancelled) setChatSyncReady(true);
      }
    };
    const hydrateTimer = window.setTimeout(hydrateChatState, 0);
    return () => { cancelled = true; window.clearTimeout(hydrateTimer); };
  }, [authSessionToken, authUser]);

  useEffect(() => { if (!authUser) return; localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authUser)); }, [authUser]);

  useEffect(() => {
    if (!authUser) return;
    localStorage.setItem(CHAT_THEME_STORAGE_KEY, chatTheme);
  }, [authUser, chatTheme]);

  useEffect(() => {
    if (!authUser || !chatSyncReady || chats.length === 0) return;
    localStorage.setItem(chatStorageKey(authUser), JSON.stringify(chats));
    const syncTimer = window.setTimeout(() => {
      chats.forEach(chat => { saveRemoteChatSession(authUser, chat).catch(error => { reportChatSyncFailure("save", error); }); });
    }, 500);
    return () => { window.clearTimeout(syncTimer); };
  }, [authUser, chatSyncReady, chats]);

  useEffect(() => {
    if (authUser && activeChatId) localStorage.setItem(activeChatKey(authUser), activeChatId);
  }, [authUser, activeChatId]);

  useEffect(() => {
    if (!inputRef.current) return;
    inputRef.current.style.height = "auto";
    inputRef.current.style.height = `${Math.min(inputRef.current.scrollHeight, 180)}px`;
  }, [input]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [activeChatId, messages.length, loadingChatId]);

  const patchChat = (chatId, patcher) => {
    setChats(prev => prev.map(chat => {
      if (chat.id !== chatId) return chat;
      const patch = patcher(chat) || {};
      return { ...chat, ...patch, updatedAt: new Date().toISOString() };
    }));
  };

  const appendMessage = (chatId, message) => {
    patchChat(chatId, chat => ({ messages: [...(chat.messages || []), message] }));
  };

  const downloadFramework = async (downloadUrl) => {
    if (!downloadUrl || !activeChat) return;
    try {
      const resolvedDownloadUrl = normalizeBackendUrl(downloadUrl);
      const response = await axios.get(resolvedDownloadUrl, { headers: { ...authHeaders(authUser) }, responseType: "blob" });
      const objectUrl = window.URL.createObjectURL(response.data);
      const anchor = document.createElement("a");
      anchor.href = objectUrl;
      anchor.download = filenameFromContentDisposition(response.headers["content-disposition"]);
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      window.setTimeout(() => window.URL.revokeObjectURL(objectUrl), 1000);
    } catch (error) {
      appendMessage(activeChat.id, {
        sender: "ai",
        text: error.response?.data?.message || error.response?.data?.error || (error.message ? `Unable to download the framework for this chat: ${error.message}.` : "Unable to download the framework for this chat."),
        type: "error"
      });
    }
  };

  const updateQueuedExecutionMessage = (chatId, job) => {
    const data = compactResponseData("generated-test-execution-queued", job);
    patchChat(chatId, chat => ({
      messages: (chat.messages || []).map(message => {
        if (message.type !== "generated-test-execution-queued" || message.data?.jobId !== data.jobId) return message;
        return { ...message, text: data.message || message.text, data: { ...message.data, ...data }, reportUrl: normalizeBackendUrl(data.reportUrl || message.reportUrl || null) };
      })
    }));
  };

  const pollGeneratedExecutionJob = (chatId, jobId) => {
    let attempts = 0;
    const poll = async () => {
      attempts += 1;
      try {
        const response = await axios.get(`${API_BASE_URL}/api/generated-test-executions/${encodeURIComponent(jobId)}`, { headers: { ...authHeaders(authUser) } });
        updateQueuedExecutionMessage(chatId, response.data);
        if (!TERMINAL_EXECUTION_STATUSES.has(response.data.status) && attempts < 240) window.setTimeout(poll, 5000);
      } catch {
        if (attempts < 5) window.setTimeout(poll, 5000);
      }
    };
    window.setTimeout(poll, 2000);
  };

  const startNewChat = () => {
    const chat = createChatSession();
    setChats(prev => [chat, ...prev]);
    setActiveChatId(chat.id);
    setInput("");
    setView("chat");
  };

  const removeChatLocally = (chatId) => {
    setChats(prev => {
      const remaining = prev.filter(chat => chat.id !== chatId);
      if (remaining.length === 0) {
        const replacement = createChatSession();
        setActiveChatId(replacement.id);
        return [replacement];
      }
      setActiveChatId(current => current === chatId ? remaining[0].id : current);
      return remaining;
    });
  };

  const deleteChat = async (event, chatId) => {
    event.stopPropagation();
    if (deletingChatIds.includes(chatId)) return;
    setDeletingChatIds(prev => prev.includes(chatId) ? prev : [...prev, chatId]);
    try {
      await axios.delete(`${API_BASE_URL}/api/workspace/sessions/${encodeURIComponent(chatId)}`, { headers: { ...authHeaders(authUser) } });
      removeChatLocally(chatId);
    } catch (error) {
      appendMessage(chatId, { sender: "ai", text: error.response?.data?.message || error.response?.data?.error || "Unable to delete this chat workspace. Try again.", type: "error" });
    } finally {
      setDeletingChatIds(prev => prev.filter(id => id !== chatId));
    }
  };

  const applyResponseToChatMetadata = (chatId, responseType, data) => {
    if (responseType !== "framework" || !data) return;
    const websiteUrl = data.websiteUrl || null;
    const domainName = data.domainName || domainFromUrl(websiteUrl);
    patchChat(chatId, chat => ({ title: domainName || chat.title, websiteUrl, domainName, frameworkLocked: true }));
  };

  const appendAiChatResponse = (chatId, responseData) => {
    const responseType = responseData.type || "info";
    const compactData = compactResponseData(responseType, responseData.data || null);
    applyResponseToChatMetadata(chatId, responseType, compactData);
    const aiMessage = {
      sender: "ai",
      text: responseData.message || "Execution completed.",
      data: compactData,
      downloadUrl: responseData.downloadUrl || compactData?.downloadUrl || compactData?.artifact?.downloadUrl || null,
      reportUrl: normalizeBackendUrl(responseData.reportUrl || compactData?.reportUrl || compactData?.reportPath || null),
      type: responseType
    };
    appendMessage(chatId, aiMessage);
    if (responseType === "generated-test-execution-queued" && compactData?.jobId) pollGeneratedExecutionJob(chatId, compactData.jobId);
  };

  const updateAuthForm = (patch) => { setAuthForm(prev => ({ ...prev, ...patch })); setAuthError(""); };

  const applyEmailPreset = (domain) => {
    setAuthForm(prev => {
      const current = prev.email.trim();
      if (!current || current.includes("@")) return prev;
      return { ...prev, email: `${current}@${domain}` };
    });
  };

  const startSocialLogin = async (provider) => {
    setAuthError(""); setSocialProvider(provider);
    if (!isSupabaseConfigured || !supabase) {
      setAuthError("Supabase is not configured yet. Add VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY, then enable the provider in Supabase Auth.");
      setSocialProvider(""); return;
    }
    const { error } = await supabase.auth.signInWithOAuth({ provider, options: { redirectTo: oauthRedirectUrl(), queryParams: provider === "google" ? { prompt: "select_account" } : undefined } });
    if (error) { setAuthError(formatOAuthError(provider, error.message)); setSocialProvider(""); }
  };

  const submitAuth = async (event) => {
    event.preventDefault(); setAuthError(""); setAuthLoading(true);
    try {
      const endpoint = authMode === "signup" ? "/api/auth/signup" : "/api/auth/login";
      const response = await axios.post(`${API_BASE_URL}${endpoint}`, authForm);
      const authenticatedUser = response.data;
      const nextState = await loadRemoteChatState(authenticatedUser);
      remoteLoadedTokenRef.current = sessionTokenForUser(authenticatedUser);
      setAuthUser(authenticatedUser);
      setChats(nextState.chats);
      setActiveChatId(nextState.activeChatId);
      setChatSyncReady(true);
      setDeletingChatIds([]);
      setInput("");
      setView("chat");
      setAuthForm({ email: "", password: "", displayName: "" });
      setShowLoginSuccess(true);
    } catch (error) {
      setAuthError(error.response?.data?.message || error.response?.data?.error || "Authentication failed.");
    } finally {
      setAuthLoading(false); setSocialProvider("");
    }
  };

  const logout = () => {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    if (isSupabaseConfigured && supabase) supabase.auth.signOut();
    remoteLoadedTokenRef.current = null;
    setAuthUser(null); setChatSyncReady(true); setChats([]); setActiveChatId(null);
    setDeletingChatIds([]); setInput(""); setView("chat");
  };

  const loadAdminMetrics = async () => {
    setAdminLoading(true); setAdminError("");
    try {
      const response = await axios.get(`${API_BASE_URL}/api/admin/metrics`, { headers: { ...authHeaders(authUser) } });
      setAdminMetrics(response.data);
    } catch (error) {
      setAdminError(error.response?.data?.message || error.response?.data?.error || "Unable to load admin metrics.");
    } finally { setAdminLoading(false); }
  };

  const openAdmin = () => { setView("admin"); loadAdminMetrics(); };

  const toggleChatTheme = () => {
    setChatTheme(current => current === "dark" ? "light" : "dark");
  };

  const uploadModifiedFramework = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file || !activeChat) return;
    setUploadingFramework(true);
    try {
      const formData = new FormData();
      formData.append("file", file);
      const response = await axios.post(`${API_BASE_URL}/api/framework/session/${activeChat.id}/upload`, formData, { headers: { ...authHeaders(authUser) } });
      const compactData = compactResponseData("framework-upload", response.data);
      patchChat(activeChat.id, chat => ({ frameworkLocked: true, messages: [...(chat.messages || []), { sender: "ai", text: response.data.message, data: compactData, type: "framework-upload" }] }));
    } catch (error) {
      appendMessage(activeChat.id, { sender: "ai", text: error.response?.data?.message || "Framework upload failed.", type: "error" });
    } finally { setUploadingFramework(false); }
  };

  const submitRuntimeVariables = async (variables) => {
    if (!activeChat || loadingChatId) return;
    const submittedVariables = Object.fromEntries(Object.entries(variables || {}).map(([key, value]) => [key.trim(), String(value ?? "")]).filter(([key, value]) => key && value.trim()));
    const variableNames = Object.keys(submittedVariables);
    if (variableNames.length === 0) return;
    const chatId = activeChat.id;
    const userText = `Submitted runtime values for ${variableNames.join(", ")}.`;
    appendMessage(chatId, { sender: "user", text: userText, type: "variables", data: savedVariableSummary(submittedVariables) });
    try {
      setLoadingChatId(chatId);
      const response = await axios.post(`${API_BASE_URL}/api/ai/chat`, { message: userText, intent: "UPDATE_TEST_DATA", variables: submittedVariables, sessionId: chatId, websiteUrl: activeChat.websiteUrl, domainName: activeChat.domainName, frameworkLocked: activeChat.frameworkLocked }, { headers: { ...authHeaders(authUser) } });
      appendAiChatResponse(chatId, response.data);
    } catch (error) {
      appendMessage(chatId, { sender: "ai", text: error.response?.data?.message || "AIF backend connection failed.", type: "error" });
    } finally {
      setLoadingChatId(current => current === chatId ? null : current);
    }
  };

  const sendMessage = async () => {
    if (!input.trim() || !activeChat || loadingChatId) return;
    const chatId = activeChat.id;
    const currentInput = input;
    appendMessage(chatId, { sender: "user", text: currentInput });
    if (activeChat.title === "New chat") patchChat(chatId, () => ({ title: titleFromMessage(currentInput) }));
    setInput("");
    try {
      setLoadingChatId(chatId);
      const response = await axios.post(`${API_BASE_URL}/api/ai/chat`, { message: currentInput, sessionId: chatId, websiteUrl: activeChat.websiteUrl, domainName: activeChat.domainName, frameworkLocked: activeChat.frameworkLocked }, { headers: { ...authHeaders(authUser) } });
      appendAiChatResponse(chatId, response.data);
    } catch (error) {
      appendMessage(chatId, { sender: "ai", text: error.response?.data?.message || "AIF backend connection failed.", type: "error" });
    } finally {
      setLoadingChatId(current => current === chatId ? null : current);
    }
  };

  if (!authUser) {
    return (
        <AuthScreen mode={authMode} form={authForm} loading={authLoading} error={authError}
                    socialProvider={socialProvider} onModeChange={setAuthMode} onFormChange={updateAuthForm}
                    onPresetDomain={applyEmailPreset} onSocialLogin={startSocialLogin} onSubmit={submitAuth} />
    );
  }

  return (
      <>
        <AnimatePresence>
          {showLoginSuccess && <LoginSuccessOverlay onDone={() => setShowLoginSuccess(false)} />}
        </AnimatePresence>

        <motion.div
            className={isChatDark ? "app app-chat-dark" : "app"}
            initial={{ opacity: 0, scale: 0.985, filter: "blur(8px)" }}
            animate={{ opacity: 1, scale: 1, filter: "blur(0px)" }}
            transition={{ duration: 0.55, ease: [0.2, 0.9, 0.3, 1], delay: showLoginSuccess ? 1.1 : 0 }}
        >
          <aside className="sidebar">
            <div className="sidebar-brand">
              <div className="brand-emblem compact">AIF</div>
              <div><h1>AIF</h1><p>Runtime Intelligence</p></div>
            </div>

            <button type="button" className="user-panel" onClick={() => setView("profile")}>
            <span className="profile-avatar">
              {authUser.avatarUrl ? <img src={authUser.avatarUrl} alt={authUser.displayName || authUser.email} /> : <FiUser />}
            </span>
              <div>
                <strong>{authUser.displayName}</strong>
                <span>{authUser.email}</span>
              </div>
            </button>

            <div className="sidebar-actions">
              <button type="button" className="new-chat-button" onClick={startNewChat}><FiPlus />New chat</button>
              <button type="button" className={view === "help" ? "sidebar-action active" : "sidebar-action"} onClick={() => setView("help")}><FiHelpCircle />Help</button>
              {authUser.role === "ADMIN" && (
                  <button type="button" className={view === "admin" ? "sidebar-action active" : "sidebar-action"} onClick={openAdmin}><FiBarChart2 />Admin</button>
              )}
            </div>

            <div className="chat-history-section">
              <div className="history-header">
                <h3>Framework sessions</h3>
                <span className="history-count">{chats.length}</span>
              </div>
              <div className="chat-history-list">
                {chats.map(chat => {
                  const isDeletingChat = deletingChatIds.includes(chat.id);
                  return (
                      <div key={chat.id} className={chat.id === activeChatId ? "chat-history-item active" : "chat-history-item"}>
                        <button type="button" className="chat-select-button" onClick={() => { setActiveChatId(chat.id); setView("chat"); }}>
                          <FiMessageSquare />
                          <span className="chat-list-text">
                        <strong>{chat.title}</strong>
                        <span>{chat.domainName || chat.websiteUrl || "No framework yet"}</span>
                      </span>
                          {chat.frameworkLocked && <FiLock className="chat-lock-icon" />}
                        </button>
                        <button type="button" className="delete-chat-button" onClick={e => deleteChat(e, chat.id)} aria-label="Delete chat" title={isDeletingChat ? "Deleting chat workspace" : "Delete chat"} disabled={isDeletingChat}>
                          <FiTrash2 />
                        </button>
                      </div>
                  );
                })}
              </div>
            </div>

            <div className="session-summary">
              <h3>Active Session</h3>
              <p>{activeChat?.websiteUrl || "Ready for a new framework"}</p>
              <span>{formatChatTime(activeChat?.updatedAt)}</span>
              <input ref={frameworkUploadRef} type="file" accept=".zip" className="hidden-file-input" onChange={uploadModifiedFramework} />
              <button type="button" className="upload-framework-button" onClick={() => frameworkUploadRef.current?.click()} disabled={uploadingFramework || !activeChat}>
                <FiUpload />{uploadingFramework ? "Uploading..." : "Upload modified framework"}
              </button>
            </div>

            <button type="button" className="logout-button" onClick={logout}><FiLogOut />Log out</button>
          </aside>

          <main className="chat-container">
            <AnimatePresence mode="wait">
              {view === "help" ? (
                  <motion.div key="help" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }} transition={{ duration: 0.2 }} className="view-fill">
                    <HelpView onBack={() => setView("chat")} />
                  </motion.div>
              ) : view === "profile" ? (
                  <motion.div key="profile" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }} transition={{ duration: 0.2 }} className="view-fill">
                    <ProfileView user={authUser} onBack={() => setView("chat")} />
                  </motion.div>
              ) : view === "admin" ? (
                  <motion.div key="admin" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }} transition={{ duration: 0.2 }} className="view-fill">
                    <AdminView metrics={adminMetrics} loading={adminLoading} error={adminError} onBack={() => setView("chat")} onRefresh={loadAdminMetrics} />
                  </motion.div>
              ) : (
                  <motion.div key="chat" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }} transition={{ duration: 0.2 }} className={isChatDark ? "chat-view chat-view-dark" : "chat-view"}>
                    <header className="chat-header">
                      <div>
                        <h2>{activeChat?.domainName || activeChat?.title || "AIF Runtime Intelligence"}</h2>
                        <span>{activeChat?.frameworkLocked ? "One chat, one website, one framework context" : "Create or select a framework session"}</span>
                      </div>
                      <div className="chat-header-actions">
                        <button type="button" className="chat-theme-toggle" onClick={toggleChatTheme} aria-label={chatThemeToggleLabel} title={chatThemeToggleLabel} aria-pressed={isChatDark}>
                          {isChatDark ? <FiSun /> : <FiMoon />}
                          <span>{isChatDark ? "Light" : "Dark"}</span>
                        </button>
                        <button type="button" className="secondary-action" onClick={() => setView("help")}><FiHelpCircle />Help</button>
                      </div>
                    </header>

                    <section className="messages">
                      {messages.map((msg, index) => (
                          <motion.div key={`${activeChat?.id || "chat"}-${index}`} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.25 }} className={msg.sender === "user" ? "message user" : "message ai"}>
                            <div className={messageContentClassName(msg)}>
                              <StructuredMessage msg={msg} onDownloadFramework={downloadFramework} onSubmitRuntimeVariables={submitRuntimeVariables} submittingRuntimeVariables={loadingChatId === activeChat?.id} />
                            </div>
                          </motion.div>
                      ))}
                      {loadingChatId === activeChatId && (
                          <div className="message ai">
                            <div className="message-content message-important thinking-box">
                              <div className="thinking-dot"></div>
                              AIF is reasoning...
                            </div>
                          </div>
                      )}
                      <div ref={messagesEndRef}></div>
                    </section>

                    <form className="input-area" onSubmit={e => { e.preventDefault(); sendMessage(); }}>
                      <div className="input-shell">
                    <textarea ref={inputRef} value={input} onChange={e => setInput(e.target.value)}
                              placeholder="Ask AIF to generate a framework, list tags, run tests, or explain a report..."
                              onKeyDown={e => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); sendMessage(); } }}
                              disabled={Boolean(loadingChatId)} rows={1} />
                        <div className="input-meta">
                          <span>Enter to send, Shift+Enter for a new line</span>
                          <span>{input.length}</span>
                        </div>
                      </div>
                      <button type="submit" disabled={Boolean(loadingChatId)} aria-label="Send" title="Send"><FiSend /></button>
                    </form>
                  </motion.div>
              )}
            </AnimatePresence>
          </main>
        </motion.div>
      </>
  );
}

export default App;
