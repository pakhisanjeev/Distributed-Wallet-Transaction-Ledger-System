#!/bin/bash
# =============================================================================
# Wallet Ledger System — API Demo (cURL Commands)
# =============================================================================
# Prerequisites:
#   1. MySQL 8.0 running on localhost:3306
#   2. Database 'wallet_ledger_db' created: CREATE DATABASE wallet_ledger_db;
#   3. Spring Boot app running on http://localhost:8080
# =============================================================================

BASE_URL="http://localhost:8080/api/v1"

echo "=============================================="
echo "  Step 1: Create Wallets"
echo "=============================================="

echo ""
echo ">>> Creating Wallet for Alice (initial balance: $1000)..."
curl -s -X POST "$BASE_URL/wallets" \
  -H "Content-Type: application/json" \
  -d '{
    "ownerName": "Alice Johnson",
    "initialBalance": 1000.00,
    "currency": "USD"
  }' | python -m json.tool

echo ""
echo ">>> Creating Wallet for Bob (initial balance: $500)..."
curl -s -X POST "$BASE_URL/wallets" \
  -H "Content-Type: application/json" \
  -d '{
    "ownerName": "Bob Smith",
    "initialBalance": 500.00,
    "currency": "USD"
  }' | python -m json.tool

echo ""
echo "=============================================="
echo "  Step 2: Query All Wallets"
echo "=============================================="

echo ""
echo ">>> Listing all wallets..."
curl -s -X GET "$BASE_URL/wallets" | python -m json.tool

echo ""
echo "=============================================="
echo "  Step 3: Transfer Funds (Alice -> Bob, $250)"
echo "=============================================="

IDEMPOTENCY_KEY="txn-$(date +%s)-001"
echo ">>> Using Idempotency Key: $IDEMPOTENCY_KEY"
echo ""

curl -s -X POST "$BASE_URL/transfers" \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d '{
    "sourceWalletId": 1,
    "destinationWalletId": 2,
    "amount": 250.00,
    "description": "Payment for services"
  }' | python -m json.tool

echo ""
echo "=============================================="
echo "  Step 4: Verify Idempotency (Replay Same Key)"
echo "=============================================="

echo ">>> Replaying the SAME idempotency key — should return cached response..."
curl -s -X POST "$BASE_URL/transfers" \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d '{
    "sourceWalletId": 1,
    "destinationWalletId": 2,
    "amount": 250.00,
    "description": "Payment for services"
  }' | python -m json.tool

echo ""
echo "=============================================="
echo "  Step 5: Check Updated Balances"
echo "=============================================="

echo ""
echo ">>> Alice's wallet (should be $750)..."
curl -s -X GET "$BASE_URL/wallets/1" | python -m json.tool

echo ""
echo ">>> Bob's wallet (should be $750)..."
curl -s -X GET "$BASE_URL/wallets/2" | python -m json.tool

echo ""
echo "=============================================="
echo "  Step 6: View Ledger Entries"
echo "=============================================="

echo ""
echo ">>> Alice's ledger (should show DEBIT of $250)..."
curl -s -X GET "$BASE_URL/wallets/1/ledger" | python -m json.tool

echo ""
echo ">>> Bob's ledger (should show CREDIT of $250)..."
curl -s -X GET "$BASE_URL/wallets/2/ledger" | python -m json.tool

echo ""
echo "=============================================="
echo "  Step 7: Error Cases"
echo "=============================================="

echo ""
echo ">>> Insufficient Balance (Alice sends $5000)..."
curl -s -X POST "$BASE_URL/transfers" \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: txn-insufficient-$(date +%s)" \
  -d '{
    "sourceWalletId": 1,
    "destinationWalletId": 2,
    "amount": 5000.00,
    "description": "This should fail"
  }' | python -m json.tool

echo ""
echo ">>> Self-Transfer (Wallet 1 -> Wallet 1)..."
curl -s -X POST "$BASE_URL/transfers" \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: txn-self-$(date +%s)" \
  -d '{
    "sourceWalletId": 1,
    "destinationWalletId": 1,
    "amount": 100.00,
    "description": "This should fail"
  }' | python -m json.tool

echo ""
echo ">>> Missing Idempotency Key..."
curl -s -X POST "$BASE_URL/transfers" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceWalletId": 1,
    "destinationWalletId": 2,
    "amount": 50.00
  }' | python -m json.tool

echo ""
echo ">>> Wallet Not Found (ID 999)..."
curl -s -X GET "$BASE_URL/wallets/999" | python -m json.tool

echo ""
echo "=============================================="
echo "  Demo Complete!"
echo "=============================================="
