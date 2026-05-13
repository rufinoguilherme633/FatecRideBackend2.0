
 package com.example.fatecCarCarona.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.example.fatecCarCarona.entity.Course;
import com.example.fatecCarCarona.entity.Destination;
import com.example.fatecCarCarona.entity.Gender;
import com.example.fatecCarCarona.entity.Origin;
import com.example.fatecCarCarona.entity.Ride;
import com.example.fatecCarCarona.entity.RideStatus;
import com.example.fatecCarCarona.entity.User;
import com.example.fatecCarCarona.entity.UserType;
import com.example.fatecCarCarona.entity.Vehicle;


@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 👈 não substituir
class RideRepositoryTest {

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTypeRepository userTypeRepository;

    @Autowired
    private GenderRepository genderRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RideStatusRepository rideStatusRepository;

    @Autowired
    private OriginRepository originRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    private User driver;
    private RideStatus status;
    private Origin origin;
    private Destination destination;
    private Vehicle vehicle;

    @BeforeEach
    void setup() {

     

        // USER TYPE
        UserType userType = new UserType();
        userType.setNome("MOTORISTA");
        userType = userTypeRepository.save(userType);

        // GENDER
        Gender gender = new Gender();
        gender.setName("MASCULINO");
        gender = genderRepository.save(gender);

        // COURSE
        Course course = new Course();
        course.setName("ADS");
        course = courseRepository.save(course);

        // DRIVER
        driver = new User();
        driver.setNome("Gui");
        driver.setSobrenome("Rufino");
        driver.setEmail("gui@email.com");
        driver.setSenha("123");
        driver.setTelefone("999999999");
        driver.setFoto("foto.png");
        driver.setUserType(userType);
        driver.setGender(gender);
        driver.setCourse(course);
        driver = userRepository.save(driver);

        // VEHICLE (🔥 ESSENCIAL)
        vehicle = new Vehicle();
        vehicle.setUser(driver);
        vehicle.setModelo("Gol");
        vehicle.setMarca("VW");
        vehicle.setPlaca("ABC-1234");
        vehicle.setCor("Preto");
        vehicle.setAno(2020);
        vehicle.setAvailableSeats(4);
        vehicle = vehicleRepository.save(vehicle);

        // STATUS
        status = new RideStatus();
        status.setNome("ativa");
        status = rideStatusRepository.save(status);

        // ORIGIN
        origin = new Origin();
        origin.setLogradouro("Rua A");
        origin.setNumero("100");
        origin.setBairro("Centro");
        origin.setCep("00000-000");
        origin.setLatitude(-23.5);
        origin.setLongitude(-46.6);
        origin = originRepository.save(origin);

        // DESTINATION
        destination = new Destination();
        destination.setLogradouro("Rua B");
        destination.setNumero("200");
        destination.setBairro("Bairro X");
        destination.setCep("11111-111");
        destination.setLatitude(-23.6);
        destination.setLongitude(-46.7);
        destination = destinationRepository.save(destination);
    }

    @Test
    void deveEncontrarCorridasAtivasPorMotorista() {

        Ride ride = new Ride();
        ride.setDriver(driver);
        ride.setVehicle(vehicle); // 🔥 FIX PRINCIPAL
        ride.setOrigin(origin);
        ride.setDestination(destination);
        ride.setStatus(status);
        ride.setAvailableSeats(3);
        ride.setDateTime(LocalDateTime.of(2026, 1, 1, 10, 0));

        rideRepository.save(ride);

        List<Ride> result = rideRepository.findAtivasByDriverId(driver.getId());

        assertNotNull(result);
        assertEquals(1, result.size());

        Ride found = result.get(0);

        result.forEach(r -> {
            System.out.println("Ride ID: " + r.getId());
            System.out.println("Driver: " + r.getDriver().getNome());
            System.out.println("Status: " + r.getStatus().getNome());
        });
        assertEquals(driver.getId(), found.getDriver().getId());
        assertEquals("Gui", found.getDriver().getNome());
        assertEquals("ativa", found.getStatus().getNome());
        assertEquals(origin.getId(), found.getOrigin().getId());
        assertEquals(destination.getId(), found.getDestination().getId());
    }


}
 