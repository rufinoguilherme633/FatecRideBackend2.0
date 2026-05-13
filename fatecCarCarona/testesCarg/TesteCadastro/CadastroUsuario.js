import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 1,
  iterations: 1,
};

export default function () {
  const payload = JSON.stringify({
    nome: "Guilherme",
    sobrenome: "Santos",
    email: "teste_unico@teste.com",
    senha: "senhaSegura123",
    telefone: "(11) 91234-5678",
    foto: "https://example.com/foto.jpg",
    userTypeId: 1,
    genderId: 2,
    courseId: 3,
    userAddressesDTO: {
      cityId: 5270,
      logradouro: "Rua Cuiabá",
      numero: "123",
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
