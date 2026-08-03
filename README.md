# Verkehrsschild Detector
**Projektarbeit Bildverarbeitung**  
HRW - Sommersemester 2026

---

Ein in Java entwickeltes Computer-Vision-System zur automatischen Erkennung und Klassifizierung von Verkehrsschilders (z.B. Stoppschilder, Vorfahrt und Vorfahrt Achten) in digitalen Bildern. 

Das Projekt nutzt klassische Bildverarbeitungsmethoden (Farbmodell-Transformationen, Kanten-Erkennung, Hough-Transformation und geometrische Invarianten) anstelle von Deep Learning.

---

## Features

* **Flexible Erkennung:** Erkennt achteckige (Stopp), viereckige (Vorfahrtsstraße) und dreieckige (Vorfahrt, Vorfahrt gewähren) Verkehrsschilder.
* **Maßstabs- & Positionstoleranz:**
  * **Image Pyramid:** Erkennt Schilder in verschiedenen Entfernungen/Größen.
  * **Moving Window:** Tastet große Bilder systematisch über ein Raster ab.
* **Robuste Farbanalyse:** Arbeiten im HSV-Farbraum zur präzisen Segmentierung unter variierenden Licht- und Schattenbedingungen.
* **Geometrische Validierung:** Analyse von Kanten, Seitenverhältnissen und Schnittpunkten zur exakten Schildtyp-Bestimmung.

---

## Funktionsweise der Pipeline

Die Verarbeitung eines Eingabebildes durchläuft folgende Stufen:

1. **Bildpyramide & Moving Window:**
   * Generierung verschiedener Skalierungsstufen des Bildes, um Schilder unabhängig von ihrer Distanz zur Kamera zu erfassen.
   * Abtastung des Bildes mit einem dynamischen Suchfenster (z. B. $200 \times 200$ Pixel).

2. **Preprocessing:**
   * Gauß Tiefpass, Histogrammausgleich.
   * Kantenfindung mit Sobelfilter und Äquidensiten
   * Morphologische Operationen (*Erosion / Dilatation*) zur Reduzierung von Hintergrundrauschen und Schließen von Kantenlücken.   

3. **Form-Erkennung (Hough-Transformation):**
   * Extraktion von Kantenlinien über die Hough-Transformation.
   * Geometrische Prüfung der drei Formen anhand der Houghlinien.

4. **Klassifizierung & Farbstatistiken:**
   * Prüfen der Farbabdeckung (Kanten des Schildes, Mitte des Schildes).

---

## Unterstützte Schildtypen

| Typ | Geometrie | Erkennungsmerkmale (HSV / Farbratios) |
| :--- | :--- | :--- |
| **Vorfahrtsstraße** | Quadrat | Weißer Rand, gelber Innenbereich |
| **Vorfahrt gewähren** | Dreieck (Spitze unten) | Roter Rand, weißer Innenbereich, **kein** schwarzes Symbol |
| **Vorfahrt** | Dreieck (Spitze oben) | Roter Rand, weißer Innenbereich, **schwarzes Piktogramm** im Zentrum |
| **Stoppschild** | Achteck | Rot/Weiß-Verhältnis |

---
