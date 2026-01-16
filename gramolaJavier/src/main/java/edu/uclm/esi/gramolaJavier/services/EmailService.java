package edu.uclm.esi.gramolaJavier.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${gramola.mail.from}")
    private String fromEmail;
    
    @Value("${gramola.mail.from.name}")
    private String fromName;
    
    public void sendConfirmationEmail(String toEmail, String barName, String confirmationUrl) {
    try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail, fromName);
        helper.setTo(toEmail);
        helper.setSubject("Confirma tu cuenta en Gramola");
        
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }
                    .container { background-color: white; padding: 30px; border-radius: 10px; max-width: 600px; margin: 0 auto; }
                    .header { text-align: center; color: #667eea; }
                    .button { display: inline-block; padding: 15px 30px; background: linear-gradient(135deg, #667eea, #764ba2); 
                             color: white; text-decoration: none; border-radius: 8px; font-weight: bold; margin: 20px 0; }
                    .footer { text-align: center; color: #999; font-size: 12px; margin-top: 30px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1 class="header">🎵 Bienvenido a Gramola</h1>
                    <p>Hola, <strong>%s</strong>!</p>
                    <p>Gracias por registrarte en Gramola. Para activar tu cuenta, haz clic en el siguiente botón:</p>
                    <div style="text-align: center;">
                        <a href="%s" class="button">Confirmar mi Cuenta</a>
                    </div>
                    <p>Este enlace expirará en <strong>30 minutos</strong>.</p>
                    <p>Si no solicitaste este registro, puedes ignorar este correo.</p>
                    <div class="footer">
                        <p>Este es un correo automático, por favor no respondas.</p>
                        <p>&copy; 2026 Gramola - Sistema de Música Interactiva</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(barName, confirmationUrl);
        
        helper.setText(htmlContent, true);
        
        mailSender.send(message);
        
        System.out.println("✅ Correo de confirmación enviado a: " + toEmail);
        System.out.println("🔗 URL de confirmación: " + confirmationUrl);
        
    } catch (Exception e) {
        System.err.println("❌ Error al enviar correo a " + toEmail + ": " + e.getMessage());
        e.printStackTrace();
    }
}

    private String buildConfirmationEmailHtml(String barName, String confirmationUrl) {
        return "<!DOCTYPE html>" +
            "<html lang='es'>" +
            "<head>" +
            "    <meta charset='UTF-8'>" +
            "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "    <style>" +
            "        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }" +
            "        .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }" +
            "        .header { background: linear-gradient(135deg, #667eea, #764ba2); color: white; padding: 40px 20px; text-align: center; }" +
            "        .header h1 { margin: 0; font-size: 28px; }" +
            "        .header p { margin: 10px 0 0; font-size: 16px; }" +
            "        .content { padding: 40px 30px; }" +
            "        .content h2 { color: #333; margin-top: 0; }" +
            "        .content p { color: #666; line-height: 1.6; }" +
            "        .button { display: inline-block; padding: 15px 30px; background: linear-gradient(135deg, #667eea, #764ba2); color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; font-weight: bold; }" +
            "        .button:hover { opacity: 0.9; }" +
            "        .footer { background: #f9f9f9; padding: 20px; text-align: center; color: #999; font-size: 12px; }" +
            "        .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }" +
            "        .warning p { margin: 0; color: #856404; }" +
            "    </style>" +
            "</head>" +
            "<body>" +
            "    <div class='container'>" +
            "        <div class='header'>" +
            "            <h1>🎵 Gramola</h1>" +
            "            <p>Sistema de Música Interactivo</p>" +
            "        </div>" +
            "        <div class='content'>" +
            "            <h2>¡Hola " + barName + "!</h2>" +
            "            <p>Gracias por registrarte en <strong>Gramola</strong>. Estamos emocionados de tenerte con nosotros.</p>" +
            "            <p>Para activar tu cuenta y comenzar a disfrutar de tu gramola interactiva, por favor confirma tu dirección de correo electrónico haciendo clic en el siguiente botón:</p>" +
            "            <div style='text-align: center;'>" +
            "                <a href='" + confirmationUrl + "' class='button'>Confirmar mi cuenta</a>" +
            "            </div>" +
            "            <p>O copia y pega este enlace en tu navegador:</p>" +
            "            <p style='background: #f5f5f5; padding: 10px; border-radius: 5px; word-break: break-all; font-size: 12px;'>" + confirmationUrl + "</p>" +
            "            <div class='warning'>" +
            "                <p>⏰ <strong>Importante:</strong> Este enlace expirará en 30 minutos.</p>" +
            "            </div>" +
            "            <p>Una vez confirmada tu cuenta, podrás:</p>" +
            "            <ul style='color: #666;'>" +
            "                <li>🎵 Reproducir música ilimitada</li>" +
            "                <li>🎸 Integrar tu cuenta de Spotify</li>" +
            "                <li>📊 Ver estadísticas en tiempo real</li>" +
            "                <li>🔧 Recibir soporte técnico 24/7</li>" +
            "            </ul>" +
            "            <p>Si no solicitaste esta cuenta, puedes ignorar este correo de forma segura.</p>" +
            "        </div>" +
            "        <div class='footer'>" +
            "            <p>Este es un correo automático, por favor no respondas a este mensaje.</p>" +
            "            <p>&copy; 2024 Gramola. Todos los derechos reservados.</p>" +
            "        </div>" +
            "    </div>" +
            "</body>" +
            "</html>";
    }

    public void sendPasswordResetEmail(String toEmail, String barName, String resetUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Recuperación de Contraseña - Gramola");
            
            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }
                        .container { background-color: white; padding: 30px; border-radius: 10px; max-width: 600px; margin: 0 auto; }
                        .header { text-align: center; color: #667eea; }
                        .button { display: inline-block; padding: 15px 30px; background: linear-gradient(135deg, #667eea, #764ba2); 
                                 color: white; text-decoration: none; border-radius: 8px; font-weight: bold; margin: 20px 0; }
                        .footer { text-align: center; color: #999; font-size: 12px; margin-top: 30px; }
                        .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1 class="header">🔐 Recuperación de Contraseña</h1>
                        <p>Hola, <strong>%s</strong>!</p>
                        <p>Hemos recibido una solicitud para restablecer tu contraseña de Gramola.</p>
                        <p>Para crear una nueva contraseña, haz clic en el siguiente botón:</p>
                        <div style="text-align: center;">
                            <a href="%s" class="button">Restablecer mi Contraseña</a>
                        </div>
                        <div class="warning">
                            <p>⏰ Este enlace expirará en <strong>24 horas</strong>.</p>
                        </div>
                        <p>Si no solicitaste este cambio, puedes ignorar este correo de forma segura. Tu contraseña no será modificada.</p>
                        <div class="footer">
                            <p>Este es un correo automático, por favor no respondas.</p>
                            <p>&copy; 2026 Gramola - Sistema de Música Interactiva</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(barName, resetUrl);
            
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
            System.out.println("✅ Correo de recuperación enviado a: " + toEmail);
            System.out.println("🔗 URL de recuperación: " + resetUrl);
            
        } catch (Exception e) {
            System.err.println("❌ Error al enviar correo de recuperación a " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al enviar el correo de recuperación", e);
        }
    }
}