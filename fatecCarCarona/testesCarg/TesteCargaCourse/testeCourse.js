import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 10,
  duration: "10s",
};

export default function () {
  const res = http.get("http://localhost:8080/courses");

  check(res, {
    "status é 200": (r) => r.status === 200,
    "tem resposta": (r) => r.body.length > 0,
  });

  sleep(1); // simula usuário real
}

/*
teste para verificar se a API de cursos está respondendo corretamente,

antes gerava alguns gargalos


agora com 100 de resposta

*/
