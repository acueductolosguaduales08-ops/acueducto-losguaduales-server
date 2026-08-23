package com.acueducto.backend.controller;

import com.acueducto.backend.entity.Factura;
import com.acueducto.backend.entity.Recibo;
import com.acueducto.backend.service.DocumentoService;
import com.acueducto.backend.service.EnlacePublicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Descarga publica de facturas y recibos mediante un enlace temporal (sin iniciar sesion).
 *
 * Flujo:
 *   /api/v1/public/{tipo}/{token}      → HTML landing page (loading → auto-descarga PDF)
 *   /api/v1/public/{tipo}/{token}/pdf   → PDF binario directo (para fetch interno desde la landing)
 */
@Tag(name = "19. Enlaces publicos de documentos", description = "Descarga publica (sin login) de facturas y recibos en PDF mediante enlace temporal compartido")
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class EnlacePublicoController {

    private final EnlacePublicoService enlacePublicoService;
    private final DocumentoService documentoService;

    // ─── Landing page HTML ──────────────────────────────────────────────

    @Operation(summary = "Landing page de descarga de factura")
    @GetMapping(value = "/facturas/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> landingFactura(@PathVariable String token) {
        return landingPage(token, "factura");
    }

    @Operation(summary = "Landing page de descarga de recibo")
    @GetMapping(value = "/recibos/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> landingRecibo(@PathVariable String token) {
        return landingPage(token, "recibo");
    }

    // ─── PDF directo ────────────────────────────────────────────────────

    @Operation(summary = "Descargar factura publica (PDF)",
            description = "Sin iniciar sesion. Valida token, vigencia y que la factura exista y no este anulada.")
    @GetMapping(value = "/facturas/{token}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarFacturaPublica(@PathVariable String token) {
        Factura factura = enlacePublicoService.obtenerFacturaPublica(token);
        byte[] pdf = documentoService.generarFacturaPdf(factura);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + factura.getNumeroFactura() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(summary = "Descargar recibo publico (PDF)",
            description = "Sin iniciar sesion. Valida token, vigencia y que el recibo exista y no este anulado.")
    @GetMapping(value = "/recibos/{token}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarReciboPublico(@PathVariable String token) {
        Recibo recibo = enlacePublicoService.obtenerReciboPublico(token);
        byte[] pdf = documentoService.generarReciboPdf(recibo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + recibo.getNumeroRecibo() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ─── Helper ─────────────────────────────────────────────────────────

    private ResponseEntity<String> landingPage(String token, String tipo) {
        String titulo = tipo.equals("factura") ? "Factura" : "Recibo";
        String html = LANDING_HTML
                .replace("{{TOKEN}}", token)
                .replace("{{TIPO}}", tipo)
                .replace("{{TIPO_TITULO}}", titulo);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .header("Cache-Control", "no-store")
                .body(html);
    }

    private static final String LANDING_HTML = """
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Descargar {{TIPO_TITULO}}</title>
  <style>
    *{margin:0;padding:0;box-sizing:border-box}
    body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;
         background:linear-gradient(135deg,#e8f4f8 0%,#d1ecf1 50%,#f0f9ff 100%);
         min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}
    .card{background:#fff;border-radius:20px;box-shadow:0 20px 60px rgba(0,0,0,.08);
          padding:48px 36px;max-width:420px;width:100%;text-align:center}
    .icon{width:80px;height:80px;border-radius:50%;display:flex;align-items:center;
          justify-content:center;margin:0 auto 24px;font-size:36px;transition:all .3s}
    .icon.done{background:#dcfce7}
    .icon.error{background:#fee2e2}
    .spinner{width:44px;height:44px;border:4px solid #e0f2fe;border-top-color:#0ea5e9;
             border-radius:50%;animation:spin .8s linear infinite;margin:0 auto 24px}
    @keyframes spin{to{transform:rotate(360deg)}}
    h1{font-size:22px;color:#1e293b;margin-bottom:8px}
    p{color:#64748b;font-size:15px;line-height:1.5;margin-bottom:8px}
    .sub{font-size:13px;color:#94a3b8;margin-bottom:24px}
    .btn{display:inline-block;padding:14px 36px;border-radius:12px;font-size:15px;
         font-weight:600;text-decoration:none;transition:all .2s;cursor:pointer;border:none}
    .btn-primary{background:#0ea5e9;color:#fff}
    .btn-primary:hover{background:#0284c7;transform:translateY(-1px);box-shadow:0 4px 12px rgba(14,165,233,.4)}
    .hidden{display:none}
    .details{font-size:13px;color:#94a3b8;margin-top:16px;line-height:1.6}
  </style>
</head>
<body>
  <div class="card">
    <div id="loading-state">
      <div class="spinner"></div>
      <h1>Preparando tu {{TIPO_TITULO}}</h1>
      <p class="sub">Esto puede tardar unos segundos mientras el servidor se activa&hellip;</p>
    </div>

    <div id="done-state" class="hidden">
      <div class="icon done">&#10003;</div>
      <h1>{{TIPO_TITULO}} lista</h1>
      <p>Tu descarga comenzar&aacute; autom&aacute;ticamente.</p>
      <div style="margin-top:20px">
        <button class="btn btn-primary" onclick="manualDownload()">Descargar de nuevo</button>
      </div>
      <p class="details">No requiere que tengas la app ni iniciar sesi&oacute;n.</p>
    </div>

    <div id="error-state" class="hidden">
      <div class="icon error">&#10007;</div>
      <h1>Enlace no disponible</h1>
      <p id="error-msg">Este enlace dej&oacute; de estar disponible.</p>
      <p class="details">Si crees que es un error, contacta al administrador.</p>
    </div>
  </div>

  <script>
    var pdfUrl = window.location.href + '/pdf';
    var token  = '{{TOKEN}}';

    function manualDownload() { window.location.href = pdfUrl; }

    function showError(msg) {
      document.getElementById('loading-state').classList.add('hidden');
      document.getElementById('error-state').classList.remove('hidden');
      if (msg) document.getElementById('error-msg').textContent = msg;
    }

    fetch(pdfUrl).then(function(res) {
      if (!res.ok) {
        return res.json().then(function(d) { throw new Error(d.mensaje || d.message || 'Error'); })
                         .catch(function(e) { throw e; });
      }
      return res.blob();
    }).then(function(blob) {
      var a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = '{{TIPO_TITULO}}-' + token.substring(0, 8) + '.pdf';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(a.href);
      document.getElementById('loading-state').classList.add('hidden');
      document.getElementById('done-state').classList.remove('hidden');
    }).catch(function(e) {
      var msg = e.message || '';
      if (msg.indexOf('Enlace') !== -1 || msg.indexOf('enlace') !== -1 || msg.indexOf('expirado') !== -1) {
        showError(msg);
      } else {
        showError('No se pudo generar el documento. Intenta de nuevo m\u00e1s tarde.');
      }
    });
  </script>
</body>
</html>
""";
}
