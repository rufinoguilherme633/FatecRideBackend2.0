package com.example.fatecCarCarona.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.fatecCarCarona.dto.RouteCoordinatesDTO;
import com.example.fatecCarCarona.entity.Ride;
import com.example.fatecCarCarona.repository.RideRepository;
import com.example.fatecCarCarona.dto.NearbyDriversDTO;

@Service
public class FindNearbyDrivers {
	private static final double RAIO_TERRA = 6371;
   	private static final double PI = Math.PI;

   	@Autowired
   	RideRepository rideRepository;

   	@Value("${carona.auto.raio-origem-km:9.0}")
   	private Double raioOrigemKm;

   	@Value("${carona.auto.raio-destino-km:1.0}")
   	private Double raioDestinoKm;

   	@Value("${carona.auto.limite-motoristas-candidatos:3}")
   	private Integer limiteCandidatos;

   	@Value("${carona.auto.peso-origem:0.8}")
   	private Double pesoOrigem;

   	@Value("${carona.auto.peso-destino:0.2}")
   	private Double pesoDestino;


	public List<NearbyDriversDTO> NearbyDriversService(RouteCoordinatesDTO routeCoordinatesDTO) throws Exception{

		//List<Ride> listRideRepository = rideRepository.findAll();
		List<Ride> listRideRepository = rideRepository.findAllActiveRides();
        List<RideComScore> motoristaProximos = new ArrayList<>();

        for(Ride i : listRideRepository) {

        	if(i.getAvailableSeats() > 0) {
            	double distanciaOrigem = this.calcularDistancia(routeCoordinatesDTO.latitudeOrigem(), routeCoordinatesDTO.longitudeOrigem(),i.getOrigin().getLatitude(), i.getOrigin().getLongitude());
            	double distanciaDestino = this.calcularDistancia(routeCoordinatesDTO.latitudeDestino(), routeCoordinatesDTO.longitudeDestino(),i.getDestination().getLatitude(), i.getDestination().getLongitude());
            	
            	if(distanciaOrigem < raioOrigemKm && distanciaDestino < raioDestinoKm) {
            		double score = (distanciaOrigem * pesoOrigem) + (distanciaDestino * pesoDestino);
            		motoristaProximos.add(new RideComScore(i, distanciaOrigem, score));
            	}
        	}
        }

        if(motoristaProximos.isEmpty()) {
        	throw new Exception("Nenhum motorista proximo");
        }

        // Ordenar por score (menor é melhor) e limitar a limiteCandidatos
        List<NearbyDriversDTO> listNearbyDrivers = motoristaProximos.stream()
        	.sorted(Comparator.comparingDouble(RideComScore::getScore))
        	.limit(limiteCandidatos)
        	.map(rideScore -> {
        		Ride ride = rideScore.getRide();
        		Double distanciaOrigem = rideScore.getDistanciaOrigem();
        		
        		return new NearbyDriversDTO(
        		ride.getId(),
                ride.getDriver().getId(),
                ride.getDriver().getNome(),
                ride.getDriver().getSobrenome(),
                ride.getDriver().getEmail(),
                ride.getDriver().getTelefone(),
                ride.getDriver().getFoto(),

                ride.getDriver().getGender().getName(),
                ride.getDriver().getCourse().getName(),

                ride.getOrigin().getCity().getNome(),
                ride.getOrigin().getLogradouro(),
                ride.getOrigin().getBairro(),
                ride.getOrigin().getLatitude(),
                ride.getOrigin().getLongitude(),
                distanciaOrigem,

                ride.getDestination().getCity().getNome(),
                ride.getDestination().getLogradouro(),
                ride.getDestination().getBairro(),
                ride.getDestination().getLatitude(),
                ride.getDestination().getLongitude(),

                ride.getVehicle().getModelo(),
                ride.getVehicle().getMarca(),
                ride.getVehicle().getPlaca(),
                ride.getVehicle().getCor(),
                ride.getVehicle().getAno(),

                ride.getAvailableSeats(),
                ride.getVehicle().getAvailableSeats() - ride.getAvailableSeats()
        	);
        })
        .toList();

        return listNearbyDrivers;
	}

	public  double calcularDistancia( double latitude1,double longitude1,double latitude2,double longitude2){
	   latitude1 = transformarEmRadianos(latitude1);
       longitude1 = transformarEmRadianos(longitude1);
       latitude2 = transformarEmRadianos(latitude2);
       longitude2 = transformarEmRadianos(longitude2);

	    double diferencaLatitude = calcularDiferencaValores( latitude1 , latitude2);
	    double diferencaLongitude = calcularDiferencaValores(longitude1 , longitude2);

	    double valorSenoLatitudeDividido2 = elevarAoQuadrado(transformarSeno(diferencaLatitude/2));


	    double valorSenoLongitudeDividido2 = elevarAoQuadrado(transformarSeno(diferencaLongitude/2));

	    double cossenoLatitude1 = transformarCosseno(latitude1);
	    double cossenoLatitude2 = transformarCosseno(latitude2);
	    double a  = Math.asin(Math.sqrt(valorSenoLatitudeDividido2 + cossenoLatitude1 * cossenoLatitude2 * valorSenoLongitudeDividido2 ));
	    double resultado = 2 * RAIO_TERRA * a ;
	    return  resultado;

	}

	public  double elevarAoQuadrado(double valor){
	    return Math.pow(valor,2);
	}

	public static double transformarSeno(double valor){
	    return  Math.sin(valor);
	}

	public  double transformarCosseno(double valor){
	    return Math.cos(valor);
	}
	public  double transformarEmRadianos(double valor){
	    double resultado = valor * (PI / 180);
	    return resultado;
	}

	public double calcularDiferencaValores(double valor1,double  valor2) {
		return valor2 - valor1;
	}

	// Classe auxiliar para armazenar Ride com seu score de proximidade
	private static class RideComScore {
		private final Ride ride;
		private final Double distanciaOrigem;
		private final Double score;

		public RideComScore(Ride ride, Double distanciaOrigem, Double score) {
			this.ride = ride;
			this.distanciaOrigem = distanciaOrigem;
			this.score = score;
		}

		public Ride getRide() {
			return ride;
		}

		public Double getDistanciaOrigem() {
			return distanciaOrigem;
		}

		public Double getScore() {
			return score;
		}
	}
}
