import { useEffect, useMemo, useRef, useState } from "react";

import axios from "axios";

import { AnimatePresence, motion } from "framer-motion";

import {
  FiArrowLeft,
  FiBarChart2,
  FiBookOpen,
  FiDatabase,
  FiExternalLink,
  FiHelpCircle,
  FiLock,
  FiLogOut,
  FiMail,
  FiMessageSquare,
  FiPlus,
  FiSend,
  FiShield,
  FiTrash2,
  FiUpload,
  FiUser
} from "react-icons/fi";

import heroArt from "./assets/hero.png";
import {
  isSupabaseConfigured,
  supabase
} from "./supabaseClient";
import "./index.css";

const AUTH_STORAGE_KEY =
  "aif.auth.session.v1";

const CHAT_STORAGE_PREFIX =
  "aif.chat.sessions.v2";

const ACTIVE_CHAT_PREFIX =
  "aif.chat.activeSession.v2";

const API_BASE_URL =
    import.meta.env.VITE_API_BASE_URL ||
    "https://aif-fpj8.onrender.com";

const welcomeMessage = {
  sender: "ai",
  text: "AIF Runtime Intelligence Ready.",
  type: "info"
};

const createChatId = () => {
  if (globalThis.crypto?.randomUUID) {
    return globalThis.crypto.randomUUID();
  }

  return `chat-${Date.now()}-${Math.random()
    .toString(16)
    .slice(2)}`;
};

const createChatSession = () => {
  const now =
    new Date().toISOString();

  return {
    id: createChatId(),
    title: "New chat",
    websiteUrl: null,
    domainName: null,
    frameworkLocked: false,
    createdAt: now,
    updatedAt: now,
    messages: [
      {
        ...welcomeMessage
      }
    ]
  };
};

const userStorageKey = (user) => {
  const key =
    user?.email
      ?.trim()
      .toLowerCase()
      .replace(/[^a-z0-9._-]+/g, "_") || "guest";

  return key;
};

const chatStorageKey = (user) =>
  `${CHAT_STORAGE_PREFIX}.${userStorageKey(user)}`;

const activeChatKey = (user) =>
  `${ACTIVE_CHAT_PREFIX}.${userStorageKey(user)}`;

const loadStoredAuth = () => {
  try {
    const stored =
      localStorage.getItem(AUTH_STORAGE_KEY);

    return stored
      ? JSON.parse(stored)
      : null;
  } catch {
    return null;
  }
};

const loadChats = (user) => {
  try {
    const stored =
      localStorage.getItem(chatStorageKey(user));

    if (!stored) {
      return [
        createChatSession()
      ];
    }

    const parsed =
      JSON.parse(stored);

    if (
      Array.isArray(parsed) &&
      parsed.length > 0
    ) {
      return parsed;
    }
  } catch {
    // Fall through to a fresh session.
  }

  return [
    createChatSession()
  ];
};

const loadInitialChatState = (user) => {
  const chats =
    loadChats(user);

  const storedActiveId =
    localStorage.getItem(activeChatKey(user));

  const activeChatId =
    chats.some(chat => chat.id === storedActiveId)
      ? storedActiveId
      : chats[0].id;

  return {
    chats,
    activeChatId
  };
};

const titleFromMessage = (text) => {
  const cleaned =
    text.trim()
      .replace(/\s+/g, " ");

  if (cleaned.length <= 38) {
    return cleaned;
  }

  return `${cleaned.slice(0, 35)}...`;
};

const formatChatTime = (value) => {
  if (!value) {
    return "";
  }

  try {
    return new Intl.DateTimeFormat(
      undefined,
      {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit"
      }
    ).format(new Date(value));
  } catch {
    return "";
  }
};

const domainFromUrl = (url) => {
  if (!url) {
    return null;
  }

  try {
    return new URL(url).hostname
      .replace(/^www\./, "");
  } catch {
    return url
      .replace("https://", "")
      .replace("http://", "")
      .replace(/^www\./, "")
      .split("/")[0];
  }
};

const compactResponseData = (type, data) => {
  if (!data) {
    return null;
  }

  if (Array.isArray(data)) {
    return data;
  }

  if (type === "framework") {
    return {
      sessionId: data.sessionId,
      frameworkPath: data.frameworkPath,
      downloadUrl: data.downloadUrl,
      flowsDetected: data.flowsDetected,
      websiteUrl: data.websiteUrl,
      domainName: data.domainName
    };
  }

  if (type === "feature") {
    return {
      featureName: data.featureName,
      frameworkPath: data.frameworkPath,
      downloadUrl: data.downloadUrl,
      variables: data.variables,
      testCaseCount: data.testCaseCount,
      testCases: data.testCases
    };
  }

  if (type === "generated-test-execution") {
    return {
      success: data.success,
      tagExpression: data.tagExpression,
      reportUrl: data.reportUrl,
      exitCode: data.exitCode
    };
  }

  if (type === "download") {
    return {
      sessionId: data.sessionId,
      downloadUrl: data.downloadUrl
    };
  }

  if (type === "framework-upload") {
    return {
      sessionId: data.sessionId,
      frameworkRoot: data.frameworkRoot,
      fileName: data.fileName,
      tags: data.tags || [],
      learningSummary: data.learningSummary
    };
  }

  if (type === "missing-variables") {
    return {
      missingVariables: data.missingVariables || [],
      example: data.example || ""
    };
  }

  return data;
};

const isSensitiveKey = (key) => {
  const lower =
    key.toLowerCase();

  return lower.includes("password") ||
    lower.includes("token") ||
    lower.includes("secret") ||
    lower.includes("otp");
};

const isHandledStructuredType = (type) => {
  return type === "tags" ||
    type === "generated-test-execution" ||
    type === "framework" ||
    type === "feature" ||
    type === "download" ||
    type === "framework-upload" ||
    type === "missing-variables" ||
    type === "session_guard" ||
    type === "variables";
};

const normalizeBackendUrl = (url) => {
  if (!url) return null;

  if (
    url.startsWith("http://") ||
    url.startsWith("https://")
  ) {
    return url;
  }

  if (url.startsWith("reports/")) {
    return `${API_BASE_URL}/api/reports/${url.split("/").pop()}`;
  }

  if (url.startsWith("/")) {
    return `${API_BASE_URL}${url}`;
  }

  return `${API_BASE_URL}/${url}`;
};

const oauthErrorFromLocation = () => {
  if (typeof window === "undefined") {
    return "";
  }

  const currentUrl =
    new URL(window.location.href);

  const hashParams =
    new URLSearchParams(
      currentUrl.hash.startsWith("#")
        ? currentUrl.hash.slice(1)
        : currentUrl.hash
    );

  return currentUrl.searchParams.get("error_description") ||
    hashParams.get("error_description") ||
    currentUrl.searchParams.get("error") ||
    hashParams.get("error") ||
    "";
};

const oauthRedirectUrl = () =>
  typeof window === "undefined"
    ? "/auth/callback"
    : `${window.location.origin}/auth/callback`;

const isOAuthCallbackPath = () =>
  typeof window !== "undefined" &&
  window.location.pathname === "/auth/callback";

const clearOAuthRoute = () => {
  if (typeof window === "undefined") {
    return;
  }

  const nextPath =
    isOAuthCallbackPath()
      ? "/"
      : window.location.pathname;

  window.history.replaceState(
    {},
    document.title,
    `${window.location.origin}${nextPath}`
  );
};

const providerLabel = (provider) =>
  provider === "google"
    ? "Google"
    : provider === "azure"
      ? "Microsoft"
      : "Social";

const formatOAuthError = (provider, message) => {
  const label =
    providerLabel(provider);

  const detail =
    message || "OAuth provider returned an error.";

  const lower =
    detail.toLowerCase();

  if (
    lower.includes("unsupported provider") ||
    lower.includes("provider is not enabled")
  ) {
    return `${label} sign in is not enabled in Supabase Auth. Enable the ${label} provider in Supabase, then add ${oauthRedirectUrl()} as an allowed redirect URL.`;
  }

  if (
    lower.includes("redirect") ||
    lower.includes("callback")
  ) {
    return `${label} sign in could not complete because the redirect URL is not allowed. Add ${oauthRedirectUrl()} in Supabase Auth URL configuration.`;
  }

  return `${label} sign in failed: ${detail}`;
};

const formatAdminDate = (value) => {
  if (!value) {
    return "Not available";
  }

  try {
    return new Intl.DateTimeFormat(
      undefined,
      {
        dateStyle: "medium",
        timeStyle: "short"
      }
    ).format(new Date(value));
  } catch {
    return value;
  }
};

const adminValue = (value) => {
  if (
    value === null ||
    value === undefined ||
    value === ""
  ) {
    return "Not available";
  }

  return String(value);
};

function AuthScreen({
  mode,
  form,
  loading,
  error,
  socialProvider,
  onModeChange,
  onFormChange,
  onPresetDomain,
  onSocialLogin,
  onSubmit
}) {
  return (
    <div className="auth-page">
      <div
        className="auth-scenery"
        aria-hidden="true"
      >
        <div className="auth-sun"></div>
        <div className="auth-mountain auth-mountain-1"></div>
        <div className="auth-mountain auth-mountain-2"></div>
        <div className="auth-mountain auth-mountain-3"></div>
        <div className="auth-device auth-device-left">
          <span>@generated</span>
          <strong>12</strong>
          <small>tags</small>
        </div>
        <div className="auth-device auth-device-center">
          <span>AIF</span>
          <strong>PASS</strong>
          <small>report ready</small>
        </div>
        <div className="auth-device auth-device-right">
          <span>@checkout</span>
          <strong>RUN</strong>
          <small>queued</small>
        </div>
      </div>

      <motion.section
        className="auth-brand"
        initial={{
          opacity: 0,
          x: -18
        }}
        animate={{
          opacity: 1,
          x: 0
        }}
        transition={{
          duration: 0.45
        }}
      >
        <div className="brand-row">
          <div className="brand-emblem">
            AIF
          </div>

          <div>
            <span>Agent Infrastructure Foundation</span>
            <h1>AIF Runtime</h1>
          </div>
        </div>

        <p>
          Generate frameworks, derive Gherkin tests, execute tags, and keep each website context isolated in its own chat.
        </p>

        <div className="auth-art-wrap">
          <img
            src={heroArt}
            alt="AIF layered automation workspace"
          />
        </div>

        <div className="auth-capabilities">
          <div>
            <FiShield />
            Session scoped framework memory
          </div>

          <div>
            <FiMessageSquare />
            Chat driven test execution
          </div>

          <div>
            <FiBookOpen />
            Built in command guidance
          </div>
        </div>
      </motion.section>

      <motion.section
        className="auth-panel"
        initial={{
          opacity: 0,
          y: 18
        }}
        animate={{
          opacity: 1,
          y: 0
        }}
        transition={{
          duration: 0.4,
          delay: 0.08
        }}
      >
        <div className="auth-tabs">
          <button
            type="button"
            className={mode === "login" ? "active" : ""}
            onClick={() => onModeChange("login")}
          >
            Log in
          </button>

          <button
            type="button"
            className={mode === "signup" ? "active" : ""}
            onClick={() => onModeChange("signup")}
          >
            Sign up
          </button>
        </div>

        <form
          className="auth-form"
          onSubmit={onSubmit}
        >
          <div className="auth-form-heading">
            <h2>
              {
                mode === "login"
                  ? "Welcome back"
                  : "Create your workspace"
              }
            </h2>

            <p>
              {
                mode === "login"
                  ? "Use your email and password to open your chat sessions."
                  : "Any email address works, including Gmail and work accounts."
              }
            </p>
          </div>

          {
            error && (
              <div className="auth-error">
                {error}
              </div>
            )
          }

          <div className="social-login-grid">
            <button
              type="button"
              onClick={() => onSocialLogin("google")}
              disabled={Boolean(socialProvider)}
            >
              <span className="provider-mark">G</span>
              {
                socialProvider === "google"
                  ? "Opening Google..."
                  : "Continue with Google"
              }
            </button>

            <button
              type="button"
              onClick={() => onSocialLogin("azure")}
              disabled={Boolean(socialProvider)}
            >
              <span className="provider-mark">M</span>
              {
                socialProvider === "azure"
                  ? "Opening Microsoft..."
                  : "Continue with Microsoft"
              }
            </button>
          </div>

          <div className="auth-divider">
            <span>Email account</span>
          </div>

          {
            mode === "signup" && (
              <label className="field-label">
                <span>Name</span>
                <div className="field-control">
                  <FiUser />
                  <input
                    value={form.displayName}
                    onChange={(event) => onFormChange({
                      displayName: event.target.value
                    })}
                    placeholder="Your name"
                    autoComplete="name"
                  />
                </div>
              </label>
            )
          }

          <label className="field-label">
            <span>Email</span>
            <div className="field-control">
              <FiMail />
              <input
                value={form.email}
                onChange={(event) => onFormChange({
                  email: event.target.value
                })}
                placeholder="name@example.com"
                type="email"
                autoComplete="email"
                required
              />
            </div>
          </label>

          <div className="email-presets">
            <button
              type="button"
              onClick={() => onPresetDomain("gmail.com")}
            >
              Gmail
            </button>

            <button
              type="button"
              onClick={() => onPresetDomain("outlook.com")}
            >
              Outlook
            </button>

            <button
              type="button"
              onClick={() => onPresetDomain("company.com")}
            >
              Work email
            </button>
          </div>

          <label className="field-label">
            <span>Password</span>
            <div className="field-control">
              <FiLock />
              <input
                value={form.password}
                onChange={(event) => onFormChange({
                  password: event.target.value
                })}
                placeholder="Minimum 8 characters"
                type="password"
                autoComplete={
                  mode === "login"
                    ? "current-password"
                    : "new-password"
                }
                required
              />
            </div>
          </label>

          <button
            type="submit"
            className="auth-submit"
            disabled={loading}
          >
            {
              loading
                ? "Working..."
                : mode === "login"
                  ? "Log in"
                  : "Create account"
            }
          </button>
        </form>
      </motion.section>
    </div>
  );
}

function HelpView({
  onBack
}) {
  return (
    <div className="help-page">
      <div className="help-header">
        <div>
          <span>AIF Knowledge Article</span>
          <h2>Command Helper</h2>
        </div>

        <button
          type="button"
          className="secondary-action"
          onClick={onBack}
        >
          <FiArrowLeft />
          Chat with AIF
        </button>
      </div>

      <div className="help-grid">
        <section className="help-section">
          <h3>Generate A Framework</h3>
          <p>
            Ask AIF to create an automation framework for a website. One chat should stay tied to one website and one framework.
          </p>
          <code>Generate framework for https://www.saucedemo.com</code>
        </section>

        <section className="help-section">
          <h3>Create Tests From Requirements</h3>
          <p>
            Paste plain English requirements after the framework exists. AIF turns them into tagged Gherkin, page object support, and step definitions.
          </p>
          <code>Generate tests for login, add backpack to cart, checkout successfully</code>
        </section>

        <section className="help-section">
          <h3>Find Generated Tags</h3>
          <p>
            Ask for available tags before execution. AIF responds with the generated tags and what each one covers.
          </p>
          <code>Can you please provide me with the tags of the generated tests?</code>
        </section>

        <section className="help-section">
          <h3>Run Tests</h3>
          <p>
            Execute one tag, multiple tags, or the complete generated suite. The chat response includes the Cucumber report link.
          </p>
          <code>Can you please run the tests with tag @checkout?</code>
          <code>Can you please run all the generated tests?</code>
        </section>

        <section className="help-section">
          <h3>Repair Failed Generated Tests</h3>
          <p>
            If a generated test fails, ask AIF to inspect the latest run. AIF checks the last execution output, updates generated Gherkin or support files when it can identify a safe repair, then tells you what changed.
          </p>
          <code>The last test failed, can you look at it again and fix it?</code>
        </section>

        <section className="help-section">
          <h3>Supply Runtime Variables</h3>
          <p>
            If a generated test needs values, give them in chat. Sensitive values are stored in the session and masked in the UI.
          </p>
          <code>Use username standard_user and password secret_sauce</code>
        </section>

        <section className="help-section">
          <h3>Session Rule</h3>
          <p>
            When a chat already owns a framework, create a new chat for a different website. This keeps generated files, reports, and test context clean.
          </p>
          <code>New chat {">"} Generate framework for another website</code>
        </section>

        <section className="help-section">
          <h3>Requirement Analysis and Test Creation</h3>
          <p>
            When user wants to create tests from requirements.
          </p>
          <code>can you create tests from the below requirements? {"-->"}Paste your requirement/requirements</code>
        </section>

      </div>
    </div>
  );
}

function ProfileView({
  user,
  onBack
}) {
  const initials =
    (user?.displayName || user?.email || "A")
      .slice(0, 1)
      .toUpperCase();

  return (
    <div className="profile-page">
      <div className="help-header">
        <div>
          <span>AIF Profile</span>
          <h2>Account</h2>
        </div>

        <button
          type="button"
          className="secondary-action"
          onClick={onBack}
        >
          <FiArrowLeft />
          Chat with AIF
        </button>
      </div>

      <div className="profile-body">
        <section className="profile-card">
          <div className="profile-avatar large">
            {
              user?.avatarUrl ? (
                <img
                  src={user.avatarUrl}
                  alt={user.displayName || user.email}
                />
              ) : (
                initials
              )
            }
          </div>

          <div>
            <h3>{user?.displayName}</h3>
            <p>{user?.email}</p>
          </div>
        </section>

        <section className="profile-details">
          <div>
            <span>Provider</span>
            <strong>{user?.provider || "email"}</strong>
          </div>

          <div>
            <span>Role</span>
            <strong>{user?.role || "USER"}</strong>
          </div>

          <div>
            <span>Session</span>
            <strong>
              {
                user?.sessionToken
                  ? "Active"
                  : "Not available"
              }
            </strong>
          </div>
        </section>
      </div>
    </div>
  );
}

function AdminView({
  metrics,
  loading,
  error,
  onBack,
  onRefresh
}) {
  const [activeMetric, setActiveMetric] =
    useState("users");

  const detailSections = [
    {
      key: "users",
      label: "Users",
      value: metrics?.users,
      hint: "Registered accounts",
      items: metrics?.userDetails || [],
      fields: item => [
        ["Email", item.email],
        ["Name", item.displayName],
        ["Role", item.role],
        ["Provider", item.provider],
        ["Created", formatAdminDate(item.createdAt)],
        ["Last login", formatAdminDate(item.lastLoginAt)]
      ]
    },
    {
      key: "authSessions",
      label: "Auth sessions",
      value: metrics?.authSessions,
      hint: "Issued login sessions",
      items: metrics?.authSessionDetails || [],
      fields: item => [
        ["User", item.userEmail],
        ["Status", item.status],
        ["Created", formatAdminDate(item.createdAt)],
        ["Expires", formatAdminDate(item.expiresAt)],
        ["Session ID", item.id]
      ]
    },
    {
      key: "generatedFrameworks",
      label: "Generated frameworks",
      value: metrics?.generatedFrameworks,
      hint: "Runnable framework folders",
      items: metrics?.generatedFrameworkDetails || [],
      fields: item => [
        ["Session", item.sessionId],
        ["File", item.name],
        ["Modified", formatAdminDate(item.modifiedAt)],
        ["Path", item.path]
      ]
    },
    {
      key: "generatedFeatures",
      label: "Generated features",
      value: metrics?.generatedFeatures,
      hint: "Gherkin files",
      items: metrics?.generatedFeatureDetails || [],
      fields: item => [
        ["Session", item.sessionId],
        ["Feature", item.name],
        ["Modified", formatAdminDate(item.modifiedAt)],
        ["Path", item.path]
      ]
    },
    {
      key: "executionReports",
      label: "Execution reports",
      value: metrics?.executionReports,
      hint: "HTML report artifacts",
      items: metrics?.executionReportDetails || [],
      fields: item => [
        ["Report", item.name],
        ["Modified", formatAdminDate(item.modifiedAt)],
        ["Path", item.path]
      ]
    },
    {
      key: "uploadedFrameworks",
      label: "Uploaded frameworks",
      value: metrics?.uploadedFrameworks,
      hint: "User-modified uploads",
      items: metrics?.uploadedFrameworkDetails || [],
      fields: item => [
        ["Session", item.sessionId],
        ["Marker", item.name],
        ["Modified", formatAdminDate(item.modifiedAt)],
        ["Path", item.path]
      ]
    }
  ];

  const selectedSection =
    detailSections.find(section => section.key === activeMetric) ||
    detailSections[0];

  return (
    <div className="admin-page">
      <div className="help-header">
        <div>
          <span>AIF Admin</span>
          <h2>Usage Metrics</h2>
        </div>

        <div className="header-actions">
          <button
            type="button"
            className="secondary-action"
            onClick={onRefresh}
            disabled={loading}
          >
            <FiBarChart2 />
            Refresh
          </button>

          <button
            type="button"
            className="secondary-action"
            onClick={onBack}
          >
            <FiArrowLeft />
            Chat with AIF
          </button>
        </div>
      </div>

      <div className="admin-body">
        {
          error && (
            <div className="auth-error">
              {error}
            </div>
          )
        }

        <div className="metric-grid">
          {
            detailSections.map(section => (
              <button
                key={section.key}
                type="button"
                className={
                  activeMetric === section.key
                    ? "metric-card active"
                    : "metric-card"
                }
                onClick={() => setActiveMetric(section.key)}
              >
                <span>{section.label}</span>
                <strong>
                  {
                    loading
                      ? "..."
                      : section.value ?? 0
                  }
                </strong>
                <small>{section.hint}</small>
              </button>
            ))
          }
        </div>

        <section className="admin-detail-panel">
          <div className="admin-detail-heading">
            <div>
              <span>Details</span>
              <h3>{selectedSection.label}</h3>
            </div>

            <strong>
              {
                loading
                  ? "Loading"
                  : `${selectedSection.items.length} records`
              }
            </strong>
          </div>

          <div className="admin-detail-list">
            {
              !loading && selectedSection.items.length === 0 && (
                <div className="admin-empty">
                  No records found for this metric.
                </div>
              )
            }

            {
              loading && (
                <div className="admin-empty">
                  Loading details...
                </div>
              )
            }

            {
              !loading && selectedSection.items.map((item, index) => (
                <article
                  className="admin-detail-row"
                  key={`${selectedSection.key}-${item.id || item.path || index}`}
                >
                  {
                    selectedSection.fields(item).map(([label, value]) => (
                      <div key={label}>
                        <span>{label}</span>
                        <strong>{adminValue(value)}</strong>
                      </div>
                    ))
                  }
                </article>
              ))
            }
          </div>
        </section>
      </div>
    </div>
  );
}

function StructuredMessage({
  msg
}) {
  return (
    <>
      <div className="plain-text">
        {msg.text}
      </div>

      {
        msg.type === "tags" &&
        msg.data?.tags && (
          <div className="tag-container">
            {
              msg.data.tags.map((tag, i) => (
                <div
                  key={i}
                  className="tag-card"
                >
                  <div className="tag-pill">
                    {tag.tag}
                  </div>

                  <div className="tag-description">
                    {tag.description}
                  </div>
                </div>
              ))
            }
          </div>
        )
      }

      {
        msg.type === "framework" &&
        msg.data && (
          <div className="execution-card">
            <div className="db-row">
              <span>Website:</span>
              <strong>
                {msg.data.domainName || msg.data.websiteUrl}
              </strong>
            </div>

            <div className="db-row">
              <span>Flows detected:</span>
              <strong>{msg.data.flowsDetected}</strong>
            </div>

            <div className="db-row">
              <span>Session:</span>
              <strong>{msg.data.sessionId}</strong>
            </div>
          </div>
        )
      }

      {
        msg.type === "feature" &&
        msg.data && (
          <div className="execution-card">
            <div className="db-row">
              <span>Feature:</span>
              <strong>{msg.data.featureName}</strong>
            </div>

            <div className="db-row">
              <span>Framework:</span>
              <strong>{msg.data.frameworkPath}</strong>
            </div>

            {
              msg.data.testCaseCount > 0 && (
                <div className="db-row">
                  <span>Test cases:</span>
                  <strong>{msg.data.testCaseCount}</strong>
                </div>
              )
            }

            {
              msg.data.testCases?.length > 0 && (
                <div className="testcase-list">
                  <div className="testcase-row testcase-header">
                    <strong>TC ID</strong>
                    <span>Story</span>
                    <p>Scenario</p>
                    <p>Test Data</p>
                    <p>Expected</p>
                  </div>

                  {
                    msg.data.testCases.map((testCase) => (
                      <div
                        key={testCase.tcId}
                        className="testcase-row"
                      >
                        <strong>{testCase.tcId}</strong>
                        <span>{testCase.userStory}</span>
                        <p>{testCase.scenario}</p>
                        <p>{testCase.testData}</p>
                        <p>{testCase.expectedResult}</p>
                      </div>
                    ))
                  }
                </div>
              )
            }
          </div>
        )
      }

      {
        msg.type === "framework-upload" &&
        msg.data && (
          <div className="execution-card">
            <div className="db-row">
              <span>Uploaded:</span>
              <strong>{msg.data.fileName}</strong>
            </div>

            <div className="db-row">
              <span>Recognized tags:</span>
              <strong>{msg.data.tags?.length || 0}</strong>
            </div>

            {
              msg.data.tags?.length > 0 && (
                <div className="tag-container compact-tags">
                  {
                    msg.data.tags.map((tag) => (
                      <div
                        key={tag.tag}
                        className="tag-pill"
                      >
                        {tag.tag}
                      </div>
                    ))
                  }
                </div>
              )
            }
          </div>
        )
      }

      {
        msg.type === "variables" &&
        msg.data && (
          <div className="variable-container">
            {
              Object.keys(msg.data).map(key => (
                <div
                  key={key}
                  className="variable-pill"
                >
                  <span>{key}</span>
                  <strong>
                    {
                      isSensitiveKey(key)
                        ? "Saved"
                        : msg.data[key]
                    }
                  </strong>
                </div>
              ))
            }
          </div>
        )
      }

      {
        msg.type === "missing-variables" &&
        msg.data && (
          <div className="missing-variable-card">
            <strong>Runtime values needed</strong>

            <div className="variable-container">
              {
                msg.data.missingVariables.map(variable => (
                  <div
                    key={variable}
                    className="variable-pill"
                  >
                    <span>{variable}</span>
                    <strong>Required</strong>
                  </div>
                ))
              }
            </div>

            {
              msg.data.example && (
                <code>
                  {msg.data.example}
                </code>
              )
            }
          </div>
        )
      }

      {
        msg.type === "generated-test-execution" &&
        msg.data && (
          <div className="execution-card">
            <div className="db-row">
              <span>Tag filter:</span>

              <strong>
                {msg.data.tagExpression || "ALL"}
              </strong>
            </div>

            <div className="db-row">
              <span>Exit code:</span>

              <strong
                className={
                  msg.data.success
                    ? "status-pass"
                    : "status-fail"
                }
              >
                {msg.data.exitCode}
              </strong>
            </div>
          </div>
        )
      }

      {
        msg.type === "session_guard" &&
        msg.data && (
          <div className="session-lock-card">
            <FiLock />

            <div>
              <span>Current framework</span>
              <strong>
                {msg.data.currentDomain || msg.data.currentWebsite}
              </strong>
            </div>

            <div>
              <span>Requested website</span>
              <strong>
                {msg.data.requestedDomain || msg.data.requestedWebsite}
              </strong>
            </div>
          </div>
        )
      }

      {
        msg.downloadUrl && (
          <a
            href={msg.downloadUrl}
            target="_blank"
            rel="noreferrer"
            className="report-link"
          >
            <FiExternalLink />
            Download Framework
          </a>
        )
      }

      {
        msg.reportUrl && (
          <a
            href={normalizeBackendUrl(msg.reportUrl)}
            target="_blank"
            rel="noreferrer"
            className="report-link"
          >
            <FiExternalLink />
            Open Execution Report
          </a>
        )
      }

      {
        Array.isArray(msg.data) && (
          <div className="db-container">
            <div className="db-title">
              <FiDatabase />
              Database Entries
            </div>

            {
              msg.data.map((item, i) => (
                <div
                  key={i}
                  className="db-card"
                >
                  <div className="db-row">
                    <span>Step:</span>
                    <strong>
                      {item.stepOrder}
                    </strong>
                  </div>

                  <div className="db-row">
                    <span>Action:</span>
                    <strong>
                      {item.action}
                    </strong>
                  </div>

                  <div className="db-row">
                    <span>Status:</span>
                    <strong
                      className={
                        item.status === "PASSED"
                          ? "status-pass"
                          : "status-fail"
                      }
                    >
                      {item.status}
                    </strong>
                  </div>

                  <div className="db-row">
                    <span>Element:</span>
                    <strong>
                      {item.elementName}
                    </strong>
                  </div>
                </div>
              ))
            }
          </div>
        )
      }

      {
        msg.data &&
        !isHandledStructuredType(msg.type) &&
        !Array.isArray(msg.data) && (
          <pre className="response-box">
            {
              JSON.stringify(
                msg.data,
                null,
                2
              )
            }
          </pre>
        )
      }
    </>
  );
}

function App() {
  const [authUser, setAuthUser] =
    useState(loadStoredAuth);

  const [authMode, setAuthMode] =
    useState("login");

  const [authForm, setAuthForm] =
    useState({
      email: "",
      password: "",
      displayName: ""
    });

  const [authLoading, setAuthLoading] =
    useState(false);

  const [socialProvider, setSocialProvider] =
    useState("");

  const [authError, setAuthError] =
    useState(() => {
      const oauthError =
        oauthErrorFromLocation();

      return oauthError
        ? formatOAuthError(
            "social",
            oauthError
          )
        : "";
    });

  const [view, setView] =
    useState("chat");

  const [initialChatState] =
    useState(() =>
      authUser
        ? loadInitialChatState(authUser)
        : {
            chats: [],
            activeChatId: null
          }
    );

  const [chats, setChats] =
    useState(initialChatState.chats);

  const [activeChatId, setActiveChatId] =
    useState(initialChatState.activeChatId);

  const [input, setInput] =
    useState("");

  const [loadingChatId, setLoadingChatId] =
    useState(null);

  const [uploadingFramework, setUploadingFramework] =
    useState(false);

  const [adminMetrics, setAdminMetrics] =
    useState(null);

  const [adminLoading, setAdminLoading] =
    useState(false);

  const [adminError, setAdminError] =
    useState("");

  const inputRef =
    useRef(null);

  const frameworkUploadRef =
    useRef(null);

  const messagesEndRef =
    useRef(null);

  const activeChat =
    useMemo(
      () => chats.find(chat => chat.id === activeChatId) || chats[0],
      [
        chats,
        activeChatId
      ]
    );

  const messages =
    activeChat?.messages || [];

  useEffect(
    () => {
      if (!oauthErrorFromLocation()) {
        return;
      }

      clearOAuthRoute();
    },
    []
  );

  useEffect(
    () => {
      if (!isSupabaseConfigured || !supabase) {
        return undefined;
      }

      let cancelled =
        false;

      const applySupabaseSession = async (session) => {
        const supabaseUser =
          session?.user;

        if (
          !supabaseUser?.email
        ) {
          return;
        }

        const metadata =
          supabaseUser.user_metadata || {};

        const response =
          await axios.post(
            `${API_BASE_URL}/api/auth/oauth-login`,
            {
              email: supabaseUser.email,
              displayName:
                metadata.full_name ||
                metadata.name ||
                supabaseUser.email,
              provider: supabaseUser.app_metadata?.provider || "supabase",
              providerUserId: supabaseUser.id,
              avatarUrl:
                metadata.avatar_url ||
                metadata.picture ||
                null
            }
          );

        if (cancelled) {
          return;
        }

        const authenticatedUser =
          response.data;

        const nextState =
          loadInitialChatState(authenticatedUser);

        setAuthUser(authenticatedUser);
        setChats(nextState.chats);
        setActiveChatId(nextState.activeChatId);
        setInput("");
        setSocialProvider("");
        setView("chat");
        clearOAuthRoute();
      };

      supabase.auth
        .getSession()
        .then(({ data }) => {
          if (!data.session) {
            if (
              isOAuthCallbackPath() &&
              !oauthErrorFromLocation()
            ) {
              setAuthError(
                `Social sign in returned without a Supabase session. Add ${oauthRedirectUrl()} to Supabase Auth redirect URLs and confirm the provider is enabled.`
              );
              setSocialProvider("");
            }

            return null;
          }

          return applySupabaseSession(data.session);
        })
        .catch((error) => {
          if (!cancelled) {
            setAuthError(
              error.response?.data?.message ||
              error.response?.data?.error ||
              error.message ||
              "Unable to restore Supabase session."
            );
            setSocialProvider("");
          }
        });

      const { data } =
        supabase.auth.onAuthStateChange(
          (event, session) => {
            if (
              event === "SIGNED_IN" ||
              event === "TOKEN_REFRESHED"
            ) {
              applySupabaseSession(session)
                .catch((error) => {
                  if (!cancelled) {
                    setAuthError(
                      error.response?.data?.message ||
                      error.response?.data?.error ||
                      error.message ||
                      "Unable to complete social sign in."
                    );
                    setSocialProvider("");
                  }
                });
            }
          }
        );

      return () => {
        cancelled = true;
        data.subscription.unsubscribe();
      };
    },
    []
  );

  useEffect(
    () => {
      if (!authUser) {
        return;
      }

      localStorage.setItem(
        AUTH_STORAGE_KEY,
        JSON.stringify(authUser)
      );
    },
    [authUser]
  );

  useEffect(
    () => {
      if (!authUser || chats.length === 0) {
        return;
      }

      localStorage.setItem(
        chatStorageKey(authUser),
        JSON.stringify(chats)
      );
    },
    [
      authUser,
      chats
    ]
  );

  useEffect(
    () => {
      if (authUser && activeChatId) {
        localStorage.setItem(
          activeChatKey(authUser),
          activeChatId
        );
      }
    },
    [
      authUser,
      activeChatId
    ]
  );

  useEffect(
    () => {
      if (!inputRef.current) {
        return;
      }

      inputRef.current.style.height = "auto";
      inputRef.current.style.height =
        `${Math.min(inputRef.current.scrollHeight, 180)}px`;
    },
    [input]
  );

  useEffect(
    () => {
      messagesEndRef.current?.scrollIntoView({
        behavior: "smooth",
        block: "end"
      });
    },
    [
      activeChatId,
      messages.length,
      loadingChatId
    ]
  );

  const patchChat = (chatId, patcher) => {
    setChats(prev => prev.map(chat => {
      if (chat.id !== chatId) {
        return chat;
      }

      const patch =
        patcher(chat) || {};

      return {
        ...chat,
        ...patch,
        updatedAt: new Date().toISOString()
      };
    }));
  };

  const appendMessage = (chatId, message) => {
    patchChat(
      chatId,
      chat => ({
        messages: [
          ...(chat.messages || []),
          message
        ]
      })
    );
  };

  const startNewChat = () => {
    const chat =
      createChatSession();

    setChats(prev => [
      chat,
      ...prev
    ]);

    setActiveChatId(chat.id);
    setInput("");
    setView("chat");
  };

  const deleteChat = (event, chatId) => {
    event.stopPropagation();

    setChats(prev => {
      const remaining =
        prev.filter(chat => chat.id !== chatId);

      if (remaining.length === 0) {
        const replacement =
          createChatSession();

        setActiveChatId(replacement.id);

        return [
          replacement
        ];
      }

      if (activeChatId === chatId) {
        setActiveChatId(remaining[0].id);
      }

      return remaining;
    });
  };

  const applyResponseToChatMetadata = (chatId, responseType, data) => {
    if (
      responseType !== "framework" ||
      !data
    ) {
      return;
    }

    const websiteUrl =
      data.websiteUrl || null;

    const domainName =
      data.domainName || domainFromUrl(websiteUrl);

    patchChat(
      chatId,
      chat => ({
        title: domainName || chat.title,
        websiteUrl,
        domainName,
        frameworkLocked: true
      })
    );
  };

  const updateAuthForm = (patch) => {
    setAuthForm(prev => ({
      ...prev,
      ...patch
    }));

    setAuthError("");
  };

  const applyEmailPreset = (domain) => {
    setAuthForm(prev => {
      const current =
        prev.email.trim();

      if (!current || current.includes("@")) {
        return prev;
      }

      return {
        ...prev,
        email: `${current}@${domain}`
      };
    });
  };

  const startSocialLogin = async (provider) => {
    setAuthError("");
    setSocialProvider(provider);

    if (!isSupabaseConfigured || !supabase) {
      setAuthError(
        "Supabase is not configured yet. Add VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY, then enable the provider in Supabase Auth."
      );
      setSocialProvider("");
      return;
    }

    const redirectTo =
      oauthRedirectUrl();

    const { error } =
      await supabase.auth.signInWithOAuth({
        provider,
        options: {
          redirectTo,
          queryParams:
            provider === "google"
              ? {
                  prompt: "select_account"
                }
              : undefined
        }
      });

    if (error) {
      setAuthError(
        formatOAuthError(
          provider,
          error.message
        )
      );
      setSocialProvider("");
    }
  };

  const submitAuth = async (event) => {
    event.preventDefault();
    setAuthError("");
    setAuthLoading(true);

    try {
      const endpoint =
        authMode === "signup"
          ? "/api/auth/signup"
          : "/api/auth/login";

      const response =
        await axios.post(
          `${API_BASE_URL}${endpoint}`,
          authForm
        );

      const authenticatedUser =
        response.data;

      const nextState =
        loadInitialChatState(authenticatedUser);

      setAuthUser(authenticatedUser);
      setChats(nextState.chats);
      setActiveChatId(nextState.activeChatId);
      setInput("");
      setView("chat");
      setAuthForm({
        email: "",
        password: "",
        displayName: ""
      });
    } catch (error) {
      setAuthError(
        error.response?.data?.message ||
        error.response?.data?.error ||
        "Authentication failed."
      );
    } finally {
      setAuthLoading(false);
      setSocialProvider("");
    }
  };

  const logout = () => {
    localStorage.removeItem(AUTH_STORAGE_KEY);

    if (isSupabaseConfigured && supabase) {
      supabase.auth.signOut();
    }

    setAuthUser(null);
    setChats([]);
    setActiveChatId(null);
    setInput("");
    setView("chat");
  };

  const loadAdminMetrics = async () => {
    setAdminLoading(true);
    setAdminError("");

    try {
      const response =
        await axios.get(
          `${API_BASE_URL}/api/admin/metrics`,
          {
            headers: {
              "X-AIF-Session": authUser?.sessionToken || ""
            }
          }
        );

      setAdminMetrics(response.data);
    } catch (error) {
      setAdminError(
        error.response?.data?.message ||
        error.response?.data?.error ||
        "Unable to load admin metrics."
      );
    } finally {
      setAdminLoading(false);
    }
  };

  const openAdmin = () => {
    setView("admin");
    loadAdminMetrics();
  };

  const uploadModifiedFramework = async (event) => {
    const file =
      event.target.files?.[0];

    event.target.value =
      "";

    if (
      !file ||
      !activeChat
    ) {

      return;
    }

    setUploadingFramework(true);

    try {
      const formData =
        new FormData();

      formData.append(
        "file",
        file
      );

      const response =
        await axios.post(
          `${API_BASE_URL}/api/framework/session/${activeChat.id}/upload`,
          formData,
          {
            headers: {
              "X-AIF-Session": authUser?.sessionToken || ""
            }
          }
        );

      const compactData =
        compactResponseData(
          "framework-upload",
          response.data
        );

      patchChat(
        activeChat.id,
        chat => ({
          frameworkLocked: true,
          messages: [
            ...(chat.messages || []),
            {
              sender: "ai",
              text: response.data.message,
              data: compactData,
              type: "framework-upload"
            }
          ]
        })
      );
    } catch (error) {
      appendMessage(
        activeChat.id,
        {
          sender: "ai",
          text:
            error.response?.data?.message ||
            "Framework upload failed.",
          type: "error"
        }
      );
    } finally {
      setUploadingFramework(false);
    }
  };

  const sendMessage = async () => {
    if (!input.trim() || !activeChat || loadingChatId) return;

    const chatId =
      activeChat.id;

    const currentInput =
      input;

    const userMessage = {
      sender: "user",
      text: currentInput
    };

    appendMessage(
      chatId,
      userMessage
    );

    if (activeChat.title === "New chat") {
      patchChat(
        chatId,
        () => ({
          title: titleFromMessage(currentInput)
        })
      );
    }

    setInput("");

    try {
      setLoadingChatId(chatId);

      const response =
        await axios.post(
          `${API_BASE_URL}/api/ai/chat`,
          {
            message: currentInput,
            sessionId: chatId,
            websiteUrl: activeChat.websiteUrl,
            domainName: activeChat.domainName,
            frameworkLocked: activeChat.frameworkLocked
          },
          {
            headers: {
              "X-AIF-Session": authUser?.sessionToken || ""
            }
          }
        );

      const responseType =
        response.data.type || "info";

      const compactData =
        compactResponseData(
          responseType,
          response.data.data || null
        );

      applyResponseToChatMetadata(
        chatId,
        responseType,
        compactData
      );

      const aiMessage = {
        sender: "ai",
        text:
          response.data.message ||
          "Execution completed.",
        data: compactData,
        downloadUrl:
          response.data.downloadUrl ||
          compactData?.downloadUrl ||
          compactData?.artifact?.downloadUrl ||
          null,
        reportUrl:
          normalizeBackendUrl(
            response.data.reportUrl ||
            compactData?.reportUrl ||
            compactData?.reportPath ||
            null
          ),
        type: responseType
      };

      appendMessage(
        chatId,
        aiMessage
      );

    } catch (error) {
      appendMessage(
        chatId,
        {
          sender: "ai",
          text:
            error.response?.data?.message ||
            "AIF backend connection failed.",
          type: "error"
        }
      );

    } finally {
      setLoadingChatId(current =>
        current === chatId
          ? null
          : current
      );
    }
  };

  if (!authUser) {
    return (
      <AuthScreen
        mode={authMode}
        form={authForm}
        loading={authLoading}
        error={authError}
        socialProvider={socialProvider}
        onModeChange={setAuthMode}
        onFormChange={updateAuthForm}
        onPresetDomain={applyEmailPreset}
        onSocialLogin={startSocialLogin}
        onSubmit={submitAuth}
      />
    );
  }

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <div className="brand-emblem compact">
            AIF
          </div>

          <div>
            <h1>AIF</h1>
            <p>Runtime Intelligence</p>
          </div>
        </div>

        <button
          type="button"
          className="user-panel"
          onClick={() => setView("profile")}
        >
          <span className="profile-avatar">
            {
              authUser.avatarUrl ? (
                <img
                  src={authUser.avatarUrl}
                  alt={authUser.displayName || authUser.email}
                />
              ) : (
                <FiUser />
              )
            }
          </span>

          <div>
            <strong>{authUser.displayName}</strong>
            <span>{authUser.email}</span>
          </div>
        </button>

        <div className="sidebar-actions">
          <button
            type="button"
            className="new-chat-button"
            onClick={startNewChat}
          >
            <FiPlus />
            New chat
          </button>

          <button
            type="button"
            className={
              view === "help"
                ? "sidebar-action active"
                : "sidebar-action"
            }
            onClick={() => setView("help")}
          >
            <FiHelpCircle />
            Help
          </button>

          {
            authUser.role === "ADMIN" && (
              <button
                type="button"
                className={
                  view === "admin"
                    ? "sidebar-action active"
                    : "sidebar-action"
                }
                onClick={openAdmin}
              >
                <FiBarChart2 />
                Admin
              </button>
            )
          }
        </div>

        <div className="chat-history-section">
          <div className="history-header">
            <h3>Chats</h3>
          </div>

          <div className="chat-history-list">
            {
              chats.map(chat => (
                <div
                  key={chat.id}
                  className={
                    chat.id === activeChatId
                      ? "chat-history-item active"
                      : "chat-history-item"
                  }
                >
                  <button
                    type="button"
                    className="chat-select-button"
                    onClick={() => {
                      setActiveChatId(chat.id);
                      setView("chat");
                    }}
                  >
                    <FiMessageSquare />

                    <span className="chat-list-text">
                      <strong>{chat.title}</strong>
                      <span>
                        {
                          chat.domainName ||
                          chat.websiteUrl ||
                          "No framework yet"
                        }
                      </span>
                    </span>

                    {
                      chat.frameworkLocked && (
                        <FiLock className="chat-lock-icon" />
                      )
                    }
                  </button>

                  <button
                    type="button"
                    className="delete-chat-button"
                    onClick={(event) => deleteChat(event, chat.id)}
                    aria-label="Delete chat"
                    title="Delete chat"
                  >
                    <FiTrash2 />
                  </button>
                </div>
              ))
            }
          </div>
        </div>

        <div className="session-summary">
          <h3>Active Session</h3>
          <p>
            {
              activeChat?.websiteUrl ||
              "Ready for a new framework"
            }
          </p>
          <span>{formatChatTime(activeChat?.updatedAt)}</span>

          <input
            ref={frameworkUploadRef}
            type="file"
            accept=".zip"
            className="hidden-file-input"
            onChange={uploadModifiedFramework}
          />

          <button
            type="button"
            className="upload-framework-button"
            onClick={() => frameworkUploadRef.current?.click()}
            disabled={uploadingFramework || !activeChat}
          >
            <FiUpload />
            {
              uploadingFramework
                ? "Uploading..."
                : "Upload modified framework"
            }
          </button>
        </div>

        <button
          type="button"
          className="logout-button"
          onClick={logout}
        >
          <FiLogOut />
          Log out
        </button>
      </aside>

      <main className="chat-container">
        <AnimatePresence mode="wait">
          {
            view === "help" ? (
              <motion.div
                key="help"
                initial={{
                  opacity: 0,
                  y: 8
                }}
                animate={{
                  opacity: 1,
                  y: 0
                }}
                exit={{
                  opacity: 0,
                  y: -8
                }}
                transition={{
                  duration: 0.2
                }}
                className="view-fill"
              >
                <HelpView onBack={() => setView("chat")} />
              </motion.div>
            ) : view === "profile" ? (
              <motion.div
                key="profile"
                initial={{
                  opacity: 0,
                  y: 8
                }}
                animate={{
                  opacity: 1,
                  y: 0
                }}
                exit={{
                  opacity: 0,
                  y: -8
                }}
                transition={{
                  duration: 0.2
                }}
                className="view-fill"
              >
                <ProfileView
                  user={authUser}
                  onBack={() => setView("chat")}
                />
              </motion.div>
            ) : view === "admin" ? (
              <motion.div
                key="admin"
                initial={{
                  opacity: 0,
                  y: 8
                }}
                animate={{
                  opacity: 1,
                  y: 0
                }}
                exit={{
                  opacity: 0,
                  y: -8
                }}
                transition={{
                  duration: 0.2
                }}
                className="view-fill"
              >
                <AdminView
                  metrics={adminMetrics}
                  loading={adminLoading}
                  error={adminError}
                  onBack={() => setView("chat")}
                  onRefresh={loadAdminMetrics}
                />
              </motion.div>
            ) : (
              <motion.div
                key="chat"
                initial={{
                  opacity: 0,
                  y: 8
                }}
                animate={{
                  opacity: 1,
                  y: 0
                }}
                exit={{
                  opacity: 0,
                  y: -8
                }}
                transition={{
                  duration: 0.2
                }}
                className="chat-view"
              >
                <header className="chat-header">
                  <div>
                    <h2>
                      {
                        activeChat?.domainName ||
                        activeChat?.title ||
                        "AIF Runtime Intelligence"
                      }
                    </h2>

                    <span>
                      {
                        activeChat?.frameworkLocked
                          ? "One chat, one website, one framework context"
                          : "Create or select a framework session"
                      }
                    </span>
                  </div>

                  <button
                    type="button"
                    className="secondary-action"
                    onClick={() => setView("help")}
                  >
                    <FiHelpCircle />
                    Help
                  </button>
                </header>

                <section className="messages">
                  {
                    messages.map((msg, index) => (
                      <motion.div
                        key={`${activeChat?.id || "chat"}-${index}`}
                        initial={{
                          opacity: 0,
                          y: 10
                        }}
                        animate={{
                          opacity: 1,
                          y: 0
                        }}
                        transition={{
                          duration: 0.25
                        }}
                        className={
                          msg.sender === "user"
                            ? "message user"
                            : "message ai"
                        }
                      >
                        <div className="message-content">
                          <StructuredMessage msg={msg} />
                        </div>
                      </motion.div>
                    ))
                  }

                  {
                    loadingChatId === activeChatId && (
                      <div className="message ai">
                        <div className="message-content thinking-box">
                          <div className="thinking-dot"></div>
                          AIF is reasoning...
                        </div>
                      </div>
                    )
                  }

                  <div ref={messagesEndRef}></div>
                </section>

                <form
                  className="input-area"
                  onSubmit={(event) => {
                    event.preventDefault();
                    sendMessage();
                  }}
                >
                  <div className="input-shell">
                    <textarea
                      ref={inputRef}
                      value={input}
                      onChange={(event) =>
                        setInput(event.target.value)
                      }
                      placeholder="Ask AIF to generate a framework, list tags, run tests, or explain a report..."
                      onKeyDown={(event) => {
                        if (
                          event.key === "Enter" &&
                          !event.shiftKey
                        ) {
                          event.preventDefault();
                          sendMessage();
                        }
                      }}
                      disabled={Boolean(loadingChatId)}
                      rows={1}
                    />

                    <div className="input-meta">
                      <span>Enter to send, Shift+Enter for a new line</span>
                      <span>{input.length}</span>
                    </div>
                  </div>

                  <button
                    type="submit"
                    disabled={Boolean(loadingChatId)}
                    aria-label="Send"
                    title="Send"
                  >
                    <FiSend />
                  </button>
                </form>
              </motion.div>
            )
          }
        </AnimatePresence>
      </main>
    </div>
  );
}

export default App;
