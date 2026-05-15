import { useState } from "react";

import axios from "axios";

import { motion } from "framer-motion";

import { FiSend } from "react-icons/fi";
import { FiExternalLink } from "react-icons/fi";
import { FiDatabase } from "react-icons/fi";
import { FiCheckCircle } from "react-icons/fi";

import "./index.css";

function App() {

  const [messages, setMessages] = useState([

    {

      sender: "ai",

      text:
        "AIF Runtime Intelligence Ready.",

      type: "info"

    }

  ]);

  const [input, setInput] =
    useState("");

  const [loading, setLoading] =
    useState(false);

  // =====================================================
  // SEND MESSAGE
  // =====================================================

  const sendMessage = async () => {

    if (!input.trim()) return;

    const userMessage = {

      sender: "user",

      text: input
    };

    setMessages(prev => [
      ...prev,
      userMessage
    ]);

    const currentInput = input;

    setInput("");

    try {

      setLoading(true);

      const response =
        await axios.post(

          "http://localhost:8080/api/ai/chat",

          {
            message: currentInput
          }

        );

      console.log(response.data);

      const aiMessage = {

        sender: "ai",

        text:
          response.data.message ||

          "Execution completed.",

        data:
          response.data.data || null,

        reportUrl:
          response.data.reportUrl || null,

        type:
          response.data.type || "info"
      };

      setMessages(prev => [

        ...prev,
        aiMessage

      ]);

    } catch (e) {

      console.error(e);

      setMessages(prev => [

        ...prev,

        {

          sender: "ai",

          text:
            "AIF backend connection failed.",

          type: "error"
        }

      ]);

    } finally {

      setLoading(false);

    }

  };

  // =====================================================
  // UI
  // =====================================================

  return (

    <div className="app">

      {/* =====================================================
          SIDEBAR
      ===================================================== */}

      <div className="sidebar">

        <div className="sidebar-content">

          <h1>AIF</h1>

          <p>
            Agent Infrastructure Foundation
          </p>

        </div>

        <div className="sidebar-sun"></div>

        <div className="mountain mountain-1"></div>
        <div className="mountain mountain-2"></div>
        <div className="mountain mountain-3"></div>

        <div className="sidebar-section">

          <h3>Capabilities</h3>

          <ul>

            <li>AI Runtime Execution</li>

            <li>Self Healing</li>

            <li>Smart Assertions</li>

            <li>Execution Reports</li>

            <li>DB Intelligence</li>

            <li>Framework Generation</li>

          </ul>

        </div>

      </div>

      {/* =====================================================
          MAIN CHAT
      ===================================================== */}

      <div className="chat-container">

        {/* =====================================================
            HEADER
        ===================================================== */}

        <div className="chat-header">

          <div>

            <h2>
              AIF Runtime Intelligence
            </h2>

            <span>
              AI Driven Automation Infrastructure
            </span>

          </div>

        </div>

        {/* =====================================================
            MESSAGES
        ===================================================== */}

        <div className="messages">

          {

            messages.map((msg, index) => (

              <motion.div

                key={index}

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

                  {/* =====================================================
                      MAIN TEXT
                  ===================================================== */}

                  <div className="plain-text">

                    {msg.text}

                  </div>

                  {/* =====================================================
                      REPORT LINK
                  ===================================================== */}

                  {

                    msg.reportUrl && (

                      <a

                        href={msg.reportUrl}

                        target="_blank"

                        rel="noreferrer"

                        className="report-link"

                      >

                        <FiExternalLink />

                        Open Execution Report

                      </a>

                    )

                  }

                  {/* =====================================================
                      DB DATA
                  ===================================================== */}

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

                  {/* =====================================================
                      NORMAL JSON
                  ===================================================== */}

                  {

                    msg.data &&
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

            loading && (

              <div className="message ai">

                <div className="message-content thinking-box">

                  <div className="thinking-dot"></div>

                  AIF is reasoning...

                </div>

              </div>

            )

          }

        </div>

        {/* =====================================================
            INPUT
        ===================================================== */}

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

          />

          <button onClick={sendMessage}>

            <FiSend />

          </button>

        </div>

      </div>

    </div>

  );

}

export default App;