const express = require("express");
const db = require("./database/dbMongodb");
require('dotenv').config();

const jwt = require("jsonwebtoken");
const bodyParser = require("body-parser");
const cors = require("cors");
const { sendMessage } = require("./service/MensagensService");
const WebSocket = require("ws");
const http = require("http");
const User = require("./models/mysqlmodels/User");
const MensagensSchema = require("./models/mongodbmodels/MensagensSchema"); // importa o model

const app = express();

app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());

// Habilita CORS para permitir requests dos dev servers (Vite/other) em
// http://localhost:3000 e http://localhost:3001
app.use(
  cors({
    origin: ["http://localhost:3000", "http://localhost:3001"],
    credentials: true,
  })
);

// Monta o router de mensagens para expor endpoints REST ao frontend
const mensagensRouter = require("./controller/MensagensController");
app.use("/api/messages", mensagensRouter);

function checkToken(token) {
  try {
    return jwt.verify(token, process.env.SECRET);
  } catch (error) {
    throw new Error("Token invalido");
  }
}

const findById = async (id) => {
  try {
    const existUser = await User.findOne({ where: { id_usuario: id } });
    return existUser;
  } catch (error) {
    console.error("Erro ao buscar usuário:", error);
    return null;
  }
};

let users = new Map();

// Cria servidor HTTP a partir do Express e anexa o WebSocket.Server a ele.
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

wss.on("connection", async (ws, req) => {
  const token = req.headers["sec-websocket-protocol"];

  if (!token) {
    console.log("Conexão recusada: token ausente");
    ws.close(401, "Token ausente");
    return;
  }

  try {
    const decoded = checkToken(token);
    const existUser = await findById(decoded.sub);

    if (!existUser) {
      ws.close(404, "usuario não encontrado");
      return;
    }

    ws.userId = existUser.id_usuario;
    users.set(existUser.id_usuario, ws);

    ws.on("message", async (message) => {
      console.log("mensagem recebida do usuario" + ws.userId);

      const data = JSON.parse(message.toString());

      const existUserReciever = await findById(data.receiver);

      if (!existUserReciever) {
        ws.send(
          JSON.stringify({
            sucesso: false,
            erro: "Usuário de destino não encontrado",
          })
        );
        return;
      }

      try {
        let messageData = {
          id_sender: ws.userId,
          id_receiver: data.receiver,
          id_solicitacao: data.id_solicitacao,
          data: data.data,
          data_atualizacao: null,
          message: data.message,
        };

        console.log("Salvando mensagem... ");
        const messageSave = await new MensagensSchema(messageData);
        await messageSave.save();

        let reciverConect = users.get(messageData.id_receiver);

        console.log(reciverConect);
        if (reciverConect) {
          reciverConect.send(
            JSON.stringify({
              sucesso: true,
              tipo: "mensagem_recebida",
              mensagem: messageData,
            })
          );
        }

        ws.send(
          JSON.stringify({
            sucesso: true,
            tipo: "mensagem_confirmada",
            mensagem: "Mensagem salva e enviada com sucesso!",
          })
        );

        console.log("✅ Mensagem salva e enviada com sucesso!");
      } catch (error) {
        console.error("Erro ao processar mensagem:", error);
        ws.send(
          JSON.stringify({
            sucesso: false,
            erro: "Erro ao salvar ou enviar mensagem.",
          })
        );
      }
    });
    ws.on("close", () => {
      users.delete(ws.userId);
      console.log(`🔌 Usuário ${ws.userId} desconectado.`);
    });
  } catch (error) {
    console.error("❌ Erro de autenticação:", error.message);
    ws.close(4003, "Token inválido");
  }
});

// Inicia servidor HTTP (REST + WebSocket na mesma porta)
// Muda a porta padrão para 9000 para evitar conflito com Vite (3000)
const REST_PORT = process.env.PORT_REST || 9000;
server.listen(REST_PORT, () => {
  console.log(`Server (REST + WS) listening on port ${REST_PORT}`);
});
