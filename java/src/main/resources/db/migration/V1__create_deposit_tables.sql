CREATE TABLE "timeDeposits" (
    id INTEGER PRIMARY KEY,
    "planType" VARCHAR(100) NOT NULL,
    days INTEGER NOT NULL,
    balance NUMERIC NOT NULL
);

CREATE TABLE withdrawals (
    id INTEGER PRIMARY KEY,
    "timeDepositId" INTEGER NOT NULL REFERENCES "timeDeposits" (id),
    amount NUMERIC NOT NULL,
    date DATE NOT NULL
);

CREATE INDEX withdrawals_deposit_idx ON withdrawals ("timeDepositId");
