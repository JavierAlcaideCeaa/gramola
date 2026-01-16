package edu.uclm.esi.gramolaJavier.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import edu.uclm.esi.gramolaJavier.Dao.PriceDao;
import edu.uclm.esi.gramolaJavier.models.Price;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private PriceDao priceDao;
    
    @Override
    public void run(String... args) throws Exception {
        if (priceDao.count() == 0) {
            System.out.println("═══════════════════════════════════");
            System.out.println("💰 INICIALIZANDO PRECIOS");
            System.out.println("═══════════════════════════════════");
            
            // Precios para cola de canciones
            Price queueAdvance = new Price(199L, "Adelantar canción en cola");
            Price queueNew = new Price(299L, "Añadir canción nueva a cola");
            
            // Precios para suscripciones de propietarios
            Price subscriptionMonthly = new Price(2999L, "Suscripción Mensual");
            Price subscriptionAnnual = new Price(29900L, "Suscripción Anual");
            
            priceDao.save(queueAdvance);
            priceDao.save(queueNew);
            priceDao.save(subscriptionMonthly);
            priceDao.save(subscriptionAnnual);
            
            System.out.println("✅ Precios inicializados:");
            System.out.println("   Cola - Adelantar: 1.99€");
            System.out.println("   Cola - Nueva: 2.99€");
            System.out.println("   Suscripción Mensual: 29.99€");
            System.out.println("   Suscripción Anual: 299€");
            System.out.println("═══════════════════════════════════");
        }
    }
}