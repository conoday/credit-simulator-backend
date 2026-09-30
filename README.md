# Credit Simulator

Aplikasi console Java 17 untuk menghitung cicilan kendaraan berdasarkan jenis, kondisi, tahun kendaraan, jumlah pinjaman, tenor, dan DP.

## Prasyarat

- Java 17 atau lebih baru
- Maven 3.9+ untuk build standar

Maven hanya dipakai saat build. Aplikasi tidak membutuhkan library runtime tambahan.

## Menjalankan aplikasi

Dari root project:

```sh
./credit_simulator
```

Windows PowerShell:

```powershell
java -cp target/classes com.nusantech.creditsimulator.Main
```

Jika `target/classes` belum ada, build dulu:

```sh
mvn package
```

## Input dari file

Perintah berikut menghitung input lalu keluar:

```sh
./credit_simulator file_inputs.txt
```

File dapat memakai salah satu format berikut.

### Format key-value

```text
vehicleType=Mobil
vehicleCondition=Bekas
vehicleYear=2024
totalLoanAmount=100000000
loanTenure=3
downPayment=25000000
```

### Format enam baris

Urutan barisnya adalah jenis kendaraan, kondisi kendaraan, tahun, jumlah pinjaman, tenor, dan DP.

```text
Mobil
Bekas
2024
100000000
3
25000000
```

JSON object dengan nama field yang sama juga diterima.

## Command console

Ketik `show` untuk melihat daftar lengkap command.

- `new` membuat kalkulasi dari input console.
- `load` mengambil JSON dari endpoint yang diberikan di soal dan menghitung hasilnya.
- `save sheet` menyimpan kalkulasi aktif ke `sheets.json`.
- `list sheets` menampilkan sheet tersimpan.
- `switch sheet <nama>` menampilkan sheet tertentu.
- `exit` keluar dari aplikasi.

`sheets.json` dibuat di current working directory dan sengaja di-ignore oleh Git.

## Aturan perhitungan

- Rate dasar Mobil 8% dan Motor 9%.
- Rate tahun berikutnya bertambah 0,1% per tahun dan tambahan 0,4% setiap dua tahun.
- Pokok awal adalah jumlah pinjaman dikurangi DP.
- Setiap tahun, saldo berjalan dikenakan rate tahun tersebut. Jumlah itu dibagi ke sisa bulan tenor.
- DP minimum kendaraan baru 35% dan kendaraan bekas 25%.
- Jumlah pinjaman maksimal Rp1.000.000.000 dan tenor maksimal 6 tahun.

Rumus ini menghasilkan skenario workbook: Mobil bekas, pinjaman Rp100.000.000, DP 25%, tenor 3 tahun menghasilkan cicilan bulanan Rp2.250.000,00; Rp2.432.250,00; dan Rp2.641.423,50.

## Test

Dengan Maven:

```sh
mvn test
```

Tanpa Maven:

```sh
./bin/test
```

Test mencakup rumus workbook, rate tahun 1-6, batas validasi, tiga format input, persistence sheet, serta HTTP 200, non-200, JSON invalid, dan timeout.

## Docker

```sh
docker build -t credit-simulator .
docker run --rm credit-simulator file_inputs.txt
docker run --rm -it credit-simulator
```

CI berada di `.github/workflows/ci.yml`. Job CI menjalankan compile, test, smoke test CLI, dan build Docker. Push image ke Docker Hub dijalankan untuk tag `v*` jika secret `DOCKERHUB_USERNAME` dan `DOCKERHUB_TOKEN` tersedia.
