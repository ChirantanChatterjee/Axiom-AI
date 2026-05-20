import { useEffect, useMemo, useState } from "react";

import axios from "axios";

import { motion } from "framer-motion";

import {
  FiDatabase,
  FiExternalLink,
  FiLock,
  FiMessageSquare,
  FiPlus,
  FiSend,
  FiTrash2
} from "react-icons/fi";

import "./index.css";

const CHAT_STORAGE_KEY =
  "aif.chat.sessions.v1";

const ACTIVE_CHAT_KEY =
  "aif.chat.activeSession.v1";

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

const loadChats = () => {
  try {
    const stored =
      localStorage.getItem(CHAT_STORAGE_KEY);

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

const loadInitialChatState = () => {
  const chats =
    loadChats();

  const storedActiveId =
    localStorage.getItem(ACTIVE_CHAT_KEY);

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
      variables: data.variables
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

function App() {

  const [initialChatState] =
    useState(loadInitialChatState);

  const [chats, setChats] =
    useState(initialChatState.chats);

  const [activeChatId, setActiveChatId] =
    useState(initialChatState.activeChatId);

  const [input, setInput] =
    useState("");

  const [loadingChatId, setLoadingChatId] =
    useState(null);

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
      localStorage.setItem(
        CHAT_STORAGE_KEY,
        JSON.stringify(chats)
      );
    },
    [chats]
  );

  useEffect(
    () => {
      if (activeChatId) {
        localStorage.setItem(
          ACTIVE_CHAT_KEY,
          activeChatId
        );
      }
    },
    [activeChatId]
  );

  // =====================================================
  // CHAT STATE
  // =====================================================

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

  // =====================================================
  // URL HELPERS
  // =====================================================

  const normalizeBackendUrl = (url) => {
    if (!url) return null;

    if (
      url.startsWith("http://") ||
      url.startsWith("https://")
    ) {
      return url;
    }

    if (url.startsWith("reports/")) {
      return `http://localhost:8080/api/reports/${url.split("/").pop()}`;
    }

    if (url.startsWith("/")) {
      return `http://localhost:8080${url}`;
    }

    return `http://localhost:8080/${url}`;
  };

  const isHandledStructuredType = (type) => {
    return type === "tags" ||
      type === "generated-test-execution" ||
      type === "framework" ||
      type === "feature" ||
      type === "download" ||
      type === "session_guard" ||
      type === "variables";
  };

  // =====================================================
  // SEND MESSAGE
  // =====================================================

  const sendMessage = async () => {
    if (!input.trim() || !activeChat) return;

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
          "http://localhost:8080/api/ai/chat",
          {
            message: currentInput,
            sessionId: chatId,
            websiteUrl: activeChat.websiteUrl,
            domainName: activeChat.domainName,
            frameworkLocked: activeChat.frameworkLocked
          }
        );

      console.log(response.data);

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

    } catch (e) {
      console.error(e);

      appendMessage(
        chatId,
        {
          sender: "ai",
          text: "AIF backend connection failed.",
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

  // =====================================================
  // UI
  // =====================================================

  return (
    <div className="app">
      <div className="sidebar">
        <div className="sidebar-content brand-block">
          <h1>AIF</h1>
          <p>Agent Infrastructure Foundation</p>
        </div>

        <div className="sidebar-sun"></div>
        <div className="mountain mountain-1"></div>
        <div className="mountain mountain-2"></div>
        <div className="mountain mountain-3"></div>

        <div className="chat-history-section">
          <div className="history-header">
            <h3>Chats</h3>

            <button
              type="button"
              className="new-chat-button"
              onClick={startNewChat}
              aria-label="New chat"
              title="New chat"
            >
              <FiPlus />
            </button>
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
                    onClick={() => setActiveChatId(chat.id)}
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

        <div className="sidebar-section session-summary">
          <h3>Active Session</h3>
          <p>
            {
              activeChat?.websiteUrl ||
              "Ready for a new framework"
            }
          </p>
          <span>
            {formatChatTime(activeChat?.updatedAt)}
          </span>
        </div>
      </div>

      <div className="chat-container">
        <div className="chat-header">
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
        </div>

        <div className="messages">
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
        </div>

        <div className="input-area">
          <input
            value={input}
            onChange={(e) =>
              setInput(e.target.value)
            }
            placeholder="Ask AIF to execute flows, inspect DB, generate frameworks..."
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                sendMessage();
              }
            }}
            disabled={Boolean(loadingChatId)}
          />

          <button
            type="button"
            onClick={sendMessage}
            disabled={Boolean(loadingChatId)}
            aria-label="Send"
          >
            <FiSend />
          </button>
        </div>
      </div>
    </div>
  );
}

export default App;
