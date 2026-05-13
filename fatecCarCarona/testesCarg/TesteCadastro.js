import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 5,
  duration: "15s",
};

export default function () {
  const email = `user${__VU}_${__ITER}_${Date.now()}@teste.com`; // ✅ agora funciona

  const payload = JSON.stringify({
    nome: "Guilherme",
    sobrenome: "Santos",
    email: email,
    senha: "senhaSegura123",
    telefone: "(11) 91234-5678",
    foto: "https://example.com/foto.jpg",
    userTypeId: 1,
    genderId: 2,
    courseId: 3,
    userAddressesDTO: {
      cityId: 5270,
      logradouro: "Rua Cuiabá",
      numero: `${__ITER}`, // 🔥 evita colisão também
      bairro: "Jardim Estela Mari",
      cep: "06703-320",
    },
  });

  const res = http.post(
    "http://localhost:8080/users/criarPassageiro",
    payload,
    {
      headers: { "Content-Type": "application/json" },
    },
  );

  check(res, {
    "status é 200 ou 201": (r) => r.status === 200 || r.status === 201,
  });

  sleep(1);
}
