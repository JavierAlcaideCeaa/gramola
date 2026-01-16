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
            
            Price standard = new Price(199L, "Standard");
            Price premium = new Price(299L, "Premium");
            
            priceDao.save(standard);
            priceDao.save(premium);
            
            System.out.println("✅ Precios inicializados:");
            System.out.println("   - Standard (199): 1.99€");
            System.out.println("   - Premium (299): 2.99€");
            System.out.println("═══════════════════════════════════");
        }
    }
}