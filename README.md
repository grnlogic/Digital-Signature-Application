# Digital Signature Application

Implementasi Digital Signature menggunakan ECDSA dan hashing dengan BLAKE3, serta invisible watermarking dengan steganografi dan visible watermarking.

## Daftar Isi

- [Gambaran Umum](#gambaran-umum)
- [Teknologi yang Digunakan](#teknologi-yang-digunakan)
- [Fitur](#fitur)
- [Instalasi](#instalasi)
- [Penggunaan API](#penggunaan-api)
- [Contoh CURL](#contoh-curl)
- [Antarmuka Pengguna](#antarmuka-pengguna)

## Gambaran Umum

Aplikasi ini menyediakan solusi tanda tangan digital berbasis web dengan fokus pada keamanan dan keabsahan dokumen. Menggunakan algoritma ECDSA untuk tanda tangan digital, BLAKE3 untuk hashing, steganografi untuk watermarking tak terlihat, dan visible watermarking untuk pemberian tanda air yang tampak, aplikasi ini memberikan cara aman untuk:

1. Menandatangani dokumen digital
2. Memverifikasi keabsahan tanda tangan
3. Melakukan tanda tangan kolektif (multi-signature) di mana dua pihak dapat menandatangani dokumen secara terpisah
4. Menyematkan watermark tersembunyi dalam gambar yang berisi informasi kepemilikan dan metadata
5. Menambahkan watermark visual yang terlihat dengan level transparansi yang dapat disesuaikan

## Teknologi yang Digunakan

- **Backend**: Spring Boot 3.4.5 (Java 17)
- **Algoritma Kriptografi**:
  - ECDSA (Elliptic Curve Digital Signature Algorithm) untuk tanda tangan digital
  - BLAKE3 untuk hashing dokumen (algoritma hash modern yang cepat dan aman)
- **Steganografi**: Implementasi LSB (Least Significant Bit) untuk watermarking tak terlihat pada gambar
- **Watermarking Visual**: Java AWT/Graphics2D untuk menambahkan watermark terlihat pada gambar
- **QR Code**: ZXing untuk generate QR Code yang berisi informasi tanda tangan

## Fitur

### 1. Tanda Tangan Digital Individual

- Generate pasangan kunci publik/privat ECDSA
- Hashing dokumen menggunakan BLAKE3
- Tanda tangan hash dengan kunci privat
- Verifikasi tanda tangan dengan kunci publik
- Support untuk berbagai jenis file (dokumen, gambar, PDF, dll)

### 2. Tanda Tangan Kolektif (Multi-Signature)

- Proses tanda tangan oleh dua pihak berbeda (Desainer dan Brand)
- Format data tanda tangan kolektif: `HASH || Signature_Desainer || Signature_Brand`
- Verifikasi keabsahan kedua tanda tangan secara bersamaan
- Ideal untuk alur kerja yang memerlukan persetujuan ganda

### 3. Invisible Watermarking (Steganografi)

- Menyematkan watermark tak terlihat pada gambar sebelum proses tanda tangan
- Watermark berisi informasi pemilik, tanggal, dan ID unik
- Dapat memverifikasi dokumen dan mengekstrak watermark secara terpisah
- Tetap efektif bahkan jika gambar di-screenshot atau di-capture
- Meminimalkan dampak pada kualitas gambar asli

### 4. Visible Watermarking

- Menambahkan watermark teks yang terlihat jelas pada gambar
- Mengatur level transparansi (opacity) watermark
- Mengatur ukuran font dari watermark
- Watermark tetap menjadi bagian dari gambar yang telah ditandatangani
- Mendukung verifikasi tanda tangan pada gambar yang telah diberi watermark
- Menambahkan efek drop shadow untuk meningkatkan visibilitas pada berbagai latar belakang

### 5. QR Code Generation

- Membuat QR Code yang berisi informasi tanda tangan digital
- QR Code berisi hash dokumen, tanda tangan, nama desainer, dan tanggal
- Memudahkan verifikasi offline dengan pemindaian QR
- Format data QR terstruktur untuk memudahkan parsing

## Instalasi

### Prasyarat

- JDK 17 atau lebih tinggi
- Maven
- Web browser modern

### Langkah-langkah

1. Clone repositori ini:

```
git clone https://github.com/username/digital-signature.git
cd "Digital Signature/digital-signature"
```

2. Build project dengan Maven:

```
./mvnw clean package
```

3. Jalankan aplikasi:

```
./mvnw spring-boot:run
```

4. Akses aplikasi web:

   - Buka folder `App` dalam browser atau gunakan web server sederhana
   - Contoh dengan Python: `python -m http.server 5500` dari dalam folder `App`
   - Akses di `http://localhost:5500`

5. Backend API akan berjalan di:
   - `http://localhost:8080`

## Penggunaan API

### Endpoint API

| Endpoint                                     | Metode | Deskripsi                               | Parameter                                                                                                                                 |
| -------------------------------------------- | ------ | --------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| `/api/signature/sign`                        | POST   | Menandatangani dokumen                  | `file`: Dokumen yang akan ditandatangani<br>`designerName`: Nama desainer (opsional)                                                      |
| `/api/signature/verify`                      | POST   | Memverifikasi tanda tangan              | `file`: Dokumen<br>`signature`: Tanda tangan                                                                                              |
| `/api/signature/signWithWatermark`           | POST   | Menandatangani dengan watermark         | `file`: Gambar<br>`ownerInfo`: Info pemilik<br>`designerName`: Nama (opsional)                                                            |
| `/api/signature/verifyWithWatermark`         | POST   | Verifikasi dengan watermark             | `file`: Gambar<br>`signature`: Tanda tangan                                                                                               |
| `/api/signature/signWithVisibleWatermark`    | POST   | Menambahkan watermark terlihat          | `file`: Gambar<br>`watermarkText`: Teks watermark<br>`opacity`: Transparansi (0.0-1.0)<br>`fontSize`: Ukuran font                         |
| `/api/signature/extractWatermark`            | POST   | Ekstrak watermark saja                  | `file`: Gambar yang memiliki watermark                                                                                                    |
| `/api/signature/signCollective`              | POST   | Tanda tangan kolektif                   | `file`: Dokumen<br>`role`: "designer"/"brand"<br>`designerSignature`: Tanda tangan designer (diperlukan jika role=brand)                  |
| `/api/signature/signCollectiveWithWatermark` | POST   | Tanda tangan kolektif dengan watermark  | `file`: Gambar<br>`role`: "designer"/"brand"<br>`ownerInfo`: Info pemilik<br>`designerSignature`: Tanda tangan designer (jika role=brand) |
| `/api/signature/verifyCollective`            | POST   | Verifikasi tanda tangan kolektif        | `file`: Dokumen<br>`signature`: Tanda tangan kolektif (format: HASH\|\|DESIGNER_SIGNATURE\|\|BRAND_SIGNATURE)                             |
| `/api/signature/generateQR`                  | POST   | Generate QR Code dari data tanda tangan | `hash`: Hash dokumen<br>`signature`: Tanda tangan<br>`designerName`: Nama desainer                                                        |
| `/api/signature/status`                      | GET    | Memeriksa status API                    | -                                                                                                                                         |

## Contoh CURL

### 1. Tanda Tangan Dokumen dengan Watermark

```bash
curl -X POST -F "file=@/path/to/image.jpg" -F "ownerInfo=John Doe" \
  http://localhost:8080/api/signature/signWithWatermark
```

### 2. Verifikasi Tanda Tangan dan Ekstrak Watermark

```bash
curl -X POST -F "file=@/path/to/image.jpg" -F "signature=SIGNATURE_STRING" \
  http://localhost:8080/api/signature/verifyWithWatermark
```

### 3. Ekstrak Watermark dari Gambar

```bash
curl -X POST -F "file=@/path/to/image.jpg" \
  http://localhost:8080/api/signature/extractWatermark
```

### 4. Menambahkan Visible Watermark

```bash
curl -X POST -F "file=@/path/to/image.jpg" -F "watermarkText=COPYRIGHT 2023" \
  -F "opacity=0.5" -F "fontSize=36" \
  http://localhost:8080/api/signature/signWithVisibleWatermark
```

### 5. Tanda Tangan Kolektif dengan Watermark

```bash
curl -X POST -F "file=@/path/to/image.jpg" -F "role=designer" -F "ownerInfo=John Doe" \
  http://localhost:8080/api/signature/signCollectiveWithWatermark
```

## Antarmuka Pengguna

Aplikasi menyediakan antarmuka web yang intuitif dengan fitur-fitur berikut:

1. **Tanda Tangan dengan Watermark**: Memungkinkan pengguna mengunggah gambar, menambahkan watermark tersembunyi, dan menandatanganinya
2. **Verifikasi**: Memungkinkan pengguna untuk memverifikasi dokumen dan mengekstrak watermark
3. **Tanda Tangan Kolektif**: Menyediakan opsi untuk tanda tangan kolaboratif dengan dua peran dan watermark
4. **Watermark Visual**: Antarmuka untuk menambahkan dan menyesuaikan watermark terlihat pada gambar

### Keunggulan Watermark Steganografi

- **Tidak Terlihat**: Watermark tidak terlihat oleh mata telanjang
- **Tahan terhadap Screenshot**: Informasi kepemilikan tetap ada bahkan jika gambar di-screenshot
- **Metadata Terenkripsi**: Berisi informasi pemilik, tanggal pembuatan, dan ID unik
- **Bukti Kepemilikan**: Memberikan bukti kuat tentang kepemilikan asli desain

### Keunggulan Visible Watermark

- **Perlindungan Visual**: Menandai gambar dengan jelas sebagai karya dengan hak cipta
- **Customizable**: Dapat disesuaikan dengan kebutuhan branding
- **Tahan terhadap Editing**: Lebih sulit dihapus dibandingkan dengan overlay biasa
- **Kombinasi Keamanan**: Dapat digabungkan dengan invisible watermark untuk perlindungan berlapis

---

Dibuat sebagai implementasi Digital Signature menggunakan ECDSA, BLAKE3, Steganografi, dan Visible Watermarking.
