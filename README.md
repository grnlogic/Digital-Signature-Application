# Digital Signature Application

Implementasi Digital Signature menggunakan ECDSA dan hashing dengan BLAKE3, serta invisible watermarking dengan steganografi.

## Daftar Isi

- [Gambaran Umum](#gambaran-umum)
- [Teknologi yang Digunakan](#teknologi-yang-digunakan)
- [Fitur](#fitur)
- [Instalasi](#instalasi)
- [Penggunaan API](#penggunaan-api)
- [Contoh CURL](#contoh-curl)
- [Antarmuka Pengguna](#antarmuka-pengguna)

## Gambaran Umum

Aplikasi ini menyediakan solusi tanda tangan digital berbasis web dengan fokus pada keamanan dan keabsahan dokumen. Menggunakan algoritma ECDSA untuk tanda tangan digital, BLAKE3 untuk hashing, dan steganografi untuk watermarking gambar, aplikasi ini memberikan cara aman untuk:

1. Menandatangani dokumen digital
2. Memverifikasi keabsahan tanda tangan
3. Melakukan tanda tangan kolektif (multi-signature) di mana dua pihak dapat menandatangani dokumen secara terpisah
4. Menyematkan watermark tersembunyi dalam gambar yang berisi informasi kepemilikan dan metadata

## Teknologi yang Digunakan

- **Backend**: Spring Boot 3.4.5 (Java 17)
- **Algoritma Kriptografi**:
  - ECDSA (Elliptic Curve Digital Signature Algorithm) untuk tanda tangan digital
  - BLAKE3 untuk hashing dokumen
- **Steganografi**: Implementasi LSB (Least Significant Bit) untuk watermarking tak terlihat pada gambar

## Fitur

### 1. Tanda Tangan Digital Individual

- Generate pasangan kunci publik/privat ECDSA
- Hashing dokumen menggunakan BLAKE3
- Tanda tangan hash dengan kunci privat
- Verifikasi tanda tangan dengan kunci publik

### 2. Tanda Tangan Kolektif (Multi-Signature)

- Proses tanda tangan oleh dua pihak berbeda (Desainer dan Brand)
- Format data tanda tangan kolektif: `HASH || Signature_Desainer || Signature_Brand`
- Verifikasi keabsahan kedua tanda tangan secara bersamaan

### 3. Invisible Watermarking (Steganografi)

- Menyematkan watermark tak terlihat pada gambar sebelum proses tanda tangan
- Watermark berisi informasi pemilik, tanggal, dan ID unik
- Dapat memverifikasi dokumen dan mengekstrak watermark secara terpisah
- Tetap efektif bahkan jika gambar di-screenshot atau di-capture

## Instalasi

### Prasyarat

- JDK 17 atau lebih tinggi
- Maven
- Web browser modern

### Langkah-langkah

1. Clone repositori ini:

```
git clone https://github.com/grnlogic/digital-signature.git
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

## Penggunaan API

### Endpoint API

| Endpoint                                     | Metode | Deskripsi                              | Parameter                                                                                                                           |
| -------------------------------------------- | ------ | -------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| `/api/signature/sign`                        | POST   | Menandatangani dokumen                 | `file`: Dokumen yang akan ditandatangani                                                                                            |
| `/api/signature/verify`                      | POST   | Memverifikasi tanda tangan             | `file`: Dokumen, `signature`: Tanda tangan                                                                                          |
| `/api/signature/signWithWatermark`           | POST   | Menandatangani dengan watermark        | `file`: Gambar, `ownerInfo`: Info pemilik, `designerName`: Nama (opsional)                                                          |
| `/api/signature/verifyWithWatermark`         | POST   | Verifikasi dengan watermark            | `file`: Gambar, `signature`: Tanda tangan                                                                                           |
| `/api/signature/extractWatermark`            | POST   | Ekstrak watermark saja                 | `file`: Gambar yang memiliki watermark                                                                                              |
| `/api/signature/signCollective`              | POST   | Tanda tangan kolektif                  | `file`: Dokumen, `role`: "designer"/"brand", `designerSignature`: Tanda tangan designer (diperlukan jika role=brand)                |
| `/api/signature/signCollectiveWithWatermark` | POST   | Tanda tangan kolektif dengan watermark | `file`: Gambar, `role`: "designer"/"brand", `ownerInfo`: Info pemilik, `designerSignature`: Tanda tangan designer (jika role=brand) |
| `/api/signature/verifyCollective`            | POST   | Verifikasi tanda tangan kolektif       | `file`: Dokumen, `signature`: Tanda tangan kolektif (format: HASH\|\|DESIGNER_SIGNATURE\|\|BRAND_SIGNATURE)                         |
| `/api/signature/status`                      | GET    | Memeriksa status API                   | -                                                                                                                                   |

## Contoh CURL

### 1. Tanda Tangan Dokumen dengan Watermark

```bash
curl -X POST -F "file=@/path/to/image.jpg" -F "ownerInfo=John Doe" http://localhost:8080/api/signature/signWithWatermark
```

### 2. Verifikasi Tanda Tangan dan Ekstrak Watermark

```bash
curl -X POST -F "file=@/path/to/image.jpg" -F "signature=SIGNATURE_STRING" http://localhost:8080/api/signature/verifyWithWatermark
```

### 3. Ekstrak Watermark dari Gambar

```bash
curl -X POST -F "file=@/path/to/image.jpg" http://localhost:8080/api/signature/extractWatermark
```

### 4. Tanda Tangan Kolektif dengan Watermark

```bash
curl -X POST -F "file=@/path/to/image.jpg" -F "role=designer" -F "ownerInfo=John Doe" http://localhost:8080/api/signature/signCollectiveWithWatermark
```

## Antarmuka Pengguna

Aplikasi menyediakan antarmuka web yang intuitif dengan fitur-fitur berikut:

1. **Tanda Tangan dengan Watermark**: Memungkinkan pengguna mengunggah gambar, menambahkan watermark tersembunyi, dan menandatanganinya
2. **Verifikasi**: Memungkinkan pengguna untuk memverifikasi dokumen dan mengekstrak watermark
3. **Tanda Tangan Kolektif**: Menyediakan opsi untuk tanda tangan kolaboratif dengan dua peran dan watermark

### Keunggulan Watermark Steganografi

- **Tidak Terlihat**: Watermark tidak terlihat oleh mata telanjang
- **Tahan terhadap Screenshot**: Informasi kepemilikan tetap ada bahkan jika gambar di-screenshot
- **Metadata Terenkripsi**: Berisi informasi pemilik, tanggal pembuatan, dan ID unik
- **Bukti Kepemilikan**: Memberikan bukti kuat tentang kepemilikan asli desain

---

Dibuat oleh Kemana Informasi sebagai implementasi Digital Signature menggunakan ECDSA, BLAKE3, dan Steganografi.
