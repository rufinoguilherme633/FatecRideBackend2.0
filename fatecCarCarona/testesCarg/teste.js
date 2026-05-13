import http from "k6/http";
import { check } from "k6";
import { __VU, __ITER } from "k6/execution";

export const options = {
  vus: 1,
  duration: "10s",
};

export default function () {
  const email = `user${__VU}_${__ITER}@teste.com`;

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

  console.log(res.status);
  console.log(res.body);

  check(res, {
    "status é 200 ou 201": (r) => r.status === 200 || r.status === 201,
  });
}
