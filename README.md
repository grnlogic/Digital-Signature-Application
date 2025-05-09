# Digital Signature Application

Implementasi Digital Signature menggunakan ECDSA dan hashing dengan BLAKE3.

## Daftar Isi
- [Gambaran Umum](#gambaran-umum)
- [Teknologi yang Digunakan](#teknologi-yang-digunakan)
- [Fitur](#fitur)
- [Instalasi](#instalasi)
- [Penggunaan API](#penggunaan-api)
- [Contoh CURL](#contoh-curl)
- [Antarmuka Pengguna](#antarmuka-pengguna)

## Gambaran Umum

Aplikasi ini menyediakan solusi tanda tangan digital berbasis web dengan fokus pada keamanan dan keabsahan dokumen. Menggunakan algoritma ECDSA untuk tanda tangan digital dan BLAKE3 untuk hashing, aplikasi ini memberikan cara aman untuk:

1. Menandatangani dokumen digital
2. Memverifikasi keabsahan tanda tangan
3. Melakukan tanda tangan kolektif (multi-signature) di mana dua pihak dapat menandatangani dokumen secara terpisah

## Teknologi yang Digunakan

- **Backend**: Spring Boot 3.4.5 (Java 17)
- **Algoritma Kriptografi**:
  - ECDSA (Elliptic Curve Digital Signature Algorithm) untuk tanda tangan digital
  - BLAKE3 untuk hashing dokumen
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

| Endpoint | Metode | Deskripsi | Parameter |
|----------|--------|-----------|-----------|
| `/api/signature/sign` | POST | Menandatangani dokumen | `file`: Dokumen yang akan ditandatangani |
| `/api/signature/verify` | POST | Memverifikasi tanda tangan | `file`: Dokumen, `signature`: Tanda tangan |
| `/api/signature/signCollective` | POST | Tanda tangan kolektif | `file`: Dokumen, `role`: "designer"/"brand", `designerSignature`: Tanda tangan designer (diperlukan jika role=brand) |
| `/api/signature/verifyCollective` | POST | Verifikasi tanda tangan kolektif | `file`: Dokumen, `signature`: Tanda tangan kolektif (format: HASH\|\|DESIGNER_SIGNATURE\|\|BRAND_SIGNATURE) |
| `/api/signature/status` | GET | Memeriksa status API | - |

## Contoh CURL

### 1. Tanda Tangan Dokumen

```bash
curl -X POST -F "file=@/path/to/document.pdf" http://localhost:8080/api/signature/sign
```

### 2. Verifikasi Tanda Tangan

```bash
curl -X POST -F "file=@/path/to/document.pdf" -F "signature=SIGNATURE_STRING" http://localhost:8080/api/signature/verify
```

### 3. Tanda Tangan sebagai Desainer

```bash
curl -X POST -F "file=@/path/to/document.pdf" -F "role=designer" http://localhost:8080/api/signature/signCollective
```

### 4. Tanda Tangan sebagai Brand

```bash
curl -X POST -F "file=@/path/to/document.pdf" -F "role=brand" -F "designerSignature=DESIGNER_SIGNATURE" http://localhost:8080/api/signature/signCollective
```

### 5. Verifikasi Tanda Tangan Kolektif

```bash
curl -X POST -F "file=@/path/to/document.pdf" -F "signature=HASH||DESIGNER_SIGNATURE||BRAND_SIGNATURE" http://localhost:8080/api/signature/verifyCollective
```

## Antarmuka Pengguna

Aplikasi menyediakan antarmuka web yang intuitif dengan tiga tab utama:

1. **Tanda Tangan**: Memungkinkan pengguna untuk mengunggah dan menandatangani dokumen
2. **Verifikasi**: Memungkinkan pengguna untuk memverifikasi dokumen dengan tanda tangan yang diberikan
3. **Tanda Tangan Kolektif**: Menyediakan opsi untuk tanda tangan kolaboratif dengan dua peran:
   - **Desainer**: Dapat menandatangani dokumen terlebih dahulu
   - **Brand**: Dapat menambahkan tanda tangannya ke dokumen yang telah ditandatangani oleh Desainer

### Cara Menggunakan Tanda Tangan Kolektif

1. **Sebagai Desainer**:
   - Pilih peran "Desainer"
   - Unggah file
   - Klik "Tanda Tangan sebagai Desainer"
   - Salin tanda tangan yang dihasilkan

2. **Sebagai Brand**:
   - Pilih peran "Brand"
   - Unggah file yang sama
   - Tempel tanda tangan Desainer
   - Klik "Tanda Tangan sebagai Brand"
   - Salin tanda tangan kolektif yang dihasilkan

3. **Untuk Verifikasi**:
   - Unggah dokumen yang sama
   - Tempel tanda tangan kolektif lengkap
   - Klik "Verifikasi Tanda Tangan Kolektif"

---

Dibuat oleh Kemana Informasi sebagai implementasi Digital Signature menggunakan ECDSA dan BLAKE3.
