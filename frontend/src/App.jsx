import { MathJax, MathJaxContext } from "better-react-mathjax";
import { useState } from "react";
import axios from "axios";
import { motion } from "framer-motion";
import { FiSend } from "react-icons/fi";

import createPlotlyComponent from "react-plotly.js/factory";
import Plotly from "plotly.js-dist-min";

const Plot = createPlotlyComponent.default(Plotly);

import "./index.css";

const mathJaxConfig = {
  loader: {
    load: ["input/tex", "output/chtml"]
  }
};

function App() {

  const [messages, setMessages] = useState([
    {
      sender: "ai",
      text: "Hello Chirantan 🌿 I am Axiom-AI."
    }
  ]);

  const [input, setInput] = useState("");

  const [loading, setLoading] = useState(false);

  // =====================================================
  // SEND MESSAGE
  // =====================================================

  const sendMessage = async () => {

    if (!input.trim()) return;

    const userMessage = {
      sender: "user",
      text: input
    };

    setMessages(prev => [...prev, userMessage]);

    const currentInput = input;

    setInput("");

    try {

      setLoading(true);

      const response = await axios.post(
        "http://localhost:8080/chat",
        {
          message: currentInput
        }
      );

      console.log("BACKEND RESPONSE:");
      console.log(response.data);

      const aiMessage = {

        sender: "ai",

        text: response.data.result || "",

        latex: response.data.latex || "",

        steps: response.data.steps || [],

        graph: response.data.graph || null
      };

      setMessages(prev => [...prev, aiMessage]);

    } catch (e) {

      console.error("FRONTEND ERROR:", e);

      setMessages(prev => [
        ...prev,
        {
          sender: "ai",
          text: "Backend connection failed."
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

    <MathJaxContext config={mathJaxConfig}>

      <div className="app">

        {/* =====================================================
            SIDEBAR
        ===================================================== */}

        <div className="sidebar sidebar-cinematic">

          <div className="sidebar-content">

            <h1>Axiom•AI</h1>

            <p>
              Intelligent Mathematical
              <br />
              Reasoning Engine
            </p>

          </div>

          <div className="sidebar-sun"></div>

          <div className="mountain mountain-1"></div>
          <div className="mountain mountain-2"></div>
          <div className="mountain mountain-3"></div>

        </div>

        {/* =====================================================
            CHAT CONTAINER
        ===================================================== */}

        <div className="chat-container">

          {/* =====================================================
              HEADER
          ===================================================== */}

          <div className="chat-header">

            <div>

              <h2>Axiom AI Assistant</h2>

              <span>
                Advanced Symbolic Mathematics
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
                        USER MESSAGE
                    ===================================================== */}

                    {
                      msg.sender === "user" ? (

                        <div>
                          {msg.text}
                        </div>

                      ) : (

                        <div className="ai-inline-response">

                          {/* =====================================================
                              STEPS
                          ===================================================== */}

                          {
                            msg.steps &&
                            msg.steps.map((step, i) => (

                              <div
                                key={i}
                                className="math-step"
                              >

                                <MathJax dynamic={true}>
                                  {`$$${step}$$`}
                                </MathJax>

                              </div>

                            ))
                          }

                          {/* =====================================================
                              FINAL ANSWER
                          ===================================================== */}

                          {
                            msg.latex && (

                              <div className="final-answer">

                                <MathJax dynamic={true}>
                                  {`$$${msg.latex}$$`}
                                </MathJax>

                              </div>
                            )
                          }

                          {/* =====================================================
                              GRAPH
                          ===================================================== */}

                          {
                            msg.graph &&
                            msg.graph.x &&
                            msg.graph.y && (

                              <div className="graph-container">

                                <Plot

                                  data={[
                                    {
                                      x: msg.graph.x,

                                      y: msg.graph.y,

                                      type: "scatter",

                                      mode: "lines",

                                      line: {
                                        color: "#1d3557",
                                        width: 3
                                      }
                                    }
                                  ]}

                                  layout={{

                                    autosize: true,

                                    height: 450,

                                    paper_bgcolor:
                                      "rgba(0,0,0,0)",

                                    plot_bgcolor:
                                      "rgba(255,255,255,0.85)",

                                    font: {
                                      color: "#1d3557"
                                    },

                                    xaxis: {
                                      gridcolor: "#d6d6d6"
                                    },

                                    yaxis: {
                                      gridcolor: "#d6d6d6"
                                    },

                                    margin: {
                                      l: 50,
                                      r: 20,
                                      t: 20,
                                      b: 50
                                    }
                                  }}

                                  config={{
                                    responsive: true
                                  }}

                                  useResizeHandler={true}

                                  style={{
                                    width: "100%",
                                    height: "100%"
                                  }}

                                />

                              </div>
                            )
                          }

                          {/* =====================================================
                              NORMAL TEXT
                          ===================================================== */}

                          {
                            msg.text && (

                              <div className="plain-text">
                                {msg.text}
                              </div>
                            )
                          }

                        </div>
                      )
                    }

                  </div>

                </motion.div>

              ))
            }

            {/* =====================================================
                LOADING
            ===================================================== */}

            {
              loading && (

                <div className="message ai">

                  <div className="message-content">
                    Thinking...
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

              placeholder="Ask Axiom-AI anything..."

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

    </MathJaxContext>
  );
}

export default App;