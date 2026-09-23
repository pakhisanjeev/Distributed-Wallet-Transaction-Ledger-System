# Distributed Wallet Transaction & Ledger System

A robust, production-ready Spring Boot backend for managing digital wallets and peer-to-peer fund transfers. Built with a focus on data integrity, concurrency safety, and clean architecture.

## 🚀 Key Features

*   **Double-Entry Atomic Ledger:** Every transfer guarantees exact tracking by creating paired DEBIT and CREDIT ledger entries within a single atomic database transaction.
*   **Concurrency & Deadlock Prevention:** Prevents race conditions and "lost updates" using database-level **Pessimistic Locking** (`SELECT ... FOR UPDATE`). Deadlocks are systematically avoided by always acquiring row locks in a deterministic order (smallest Wallet ID first).
*   **Idempotency (Exactly-Once Semantics):** Safely handles network retries and duplicate requests. An `IdempotencyInterceptor` checks the `X-Idempotency-Key` header, caching successful responses and rejecting concurrent identical requests with HTTP 409 Conflict.
*   **Database-Level Integrity:** Uses `CHECK` constraints directly in MySQL (e.g., `CHECK (balance >= 0)`) as a final line of defense against negative balances or logic bugs.
*   **Global Exception Handling:** Clean, standardized JSON error responses for all API endpoints.

## 🛠️ Tech Stack

*   **Language:** Java 17
*   **Framework:** Spring Boot 3.x, Spring Web, Spring Data JPA
*   **Database:** MySQL 8.0
*   **Tools:** Maven, Lombok
*   **Demo Frontend:** React 18 (Standalone, no build required)

## 📋 Prerequisites

*   Java 17 or higher
*   Maven (or use an IDE with built-in Maven support)
*   MySQL 8.0 running locally on port 3306

## ⚙️ Setup & Installation

1.  **Database Setup:**
    Create a new database in your local MySQL instance:
    ```sql
    CREATE DATABASE wallet_ledger_db;
    ```
    *(The application uses default credentials: username `root`, password `root`. Update `src/main/resources/application.yml` if your credentials differ.)*

2.  **Clone the Repository:**
    ```bash
    git clone https://github.com/pakhisanjeev/Distributed-Wallet-Transaction-Ledger-System.git
    cd Distributed-Wallet-Transaction-Ledger-System
    ```

3.  **Run the Application:**
    The application will automatically initialize the database schema via `schema.sql` on startup.
    ```bash
    mvn spring-boot:run
    ```
    The server will start on `http://localhost:8080`.

## 🎮 How to Demo

We've provided three ways to test and interact with the API:

### 1. Interactive React Dashboard
Open the file `frontend/index.html` directly in any web browser. It provides a visual dashboard to create wallets, execute transfers, and view ledger history. (No `npm install` needed!).

### 2. cURL Script
Run the automated shell script to see a full flow in your terminal:
```bash
./api-demo/curl-commands.sh
```

### 3. Postman Collection
Import the `api-demo/WalletLedgerSystem.postman_collection.json` file into Postman to explore all endpoints, including idempotency tests and error cases (like insufficient balances).

## 🔌 API Endpoints Summary

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/wallets` | Create a new wallet |
| `GET` | `/api/v1/wallets` | List all wallets |
| `GET` | `/api/v1/wallets/{id}` | Get wallet details by ID |
| `GET` | `/api/v1/wallets/{id}/ledger` | View transaction history for a wallet |
| `POST` | `/api/v1/transfers` | Execute a fund transfer (Requires `X-Idempotency-Key` header) |
