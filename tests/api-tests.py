#!/usr/bin/python3

import time

from requests import Session

ENDPOINT = 'http://localhost:8500/v1'
next_token_id = int(time.time())
session = Session()
client_id = ''


def create_session():
    global session
    response = session.post('{}/sessions/create'.format(ENDPOINT))
    assert response.status_code == 200
    return response.content.decode("utf-8")

def get_next_token():
    global next_token_id
    next_token_id += 1
    return str(next_token_id)


def _create_account(token, amount, session_id=None):
    global session, client_id
    return session.post('{}/accounts/create?session_id={}&token={}&amount={}'.format(
        ENDPOINT, session_id if session_id else client_id, token, amount))


def create_account(amount, token=None, session_id=None):
    response = _create_account(
        token if token else get_next_token(), amount, session_id=session_id)
    assert response.status_code == 200
    account_id = response.content.decode("utf-8")
    assert account_id.startswith('A')
    assert len(account_id) >= 5
    print("account created", account_id)
    return account_id


def _create_transaction(token, src_id, dst_id, amount, session_id=None):
    global session, client_id
    return session.post(
        '{}/transactions/create?session_id={}&token={}&src_account_id={}&dst_account_id={}&amount={}'.format(
            ENDPOINT, session_id if session_id else client_id, token, src_id, dst_id, amount))


def create_transaction(src_id, dst_id, amount, token=None, session_id=None):
    response = _create_transaction(
        token if token else get_next_token(), src_id, dst_id, amount, session_id=session_id)
    assert response.status_code == 200
    transaction_id = response.content.decode("utf-8")
    assert transaction_id.startswith('T')
    assert len(transaction_id) >= 5
    print("transaction created", transaction_id)
    return transaction_id


def get_amount(account_id, session_id=None):
    global session, client_id
    response = session.post(
        '{}/account/amount?session_id={}&account_id={}'.format(
            ENDPOINT, session_id if session_id else client_id, account_id))
    assert response.status_code == 200
    amount = response.content.decode("utf-8")
    print("account", account_id, "amount", amount)
    return amount


def test_transaction_ok():
    print("*** test_transaction_ok: started")
    account_id_1 = create_account(1000.50)
    assert get_amount(account_id_1) == '1000.50'

    account_id_2 = create_account(2000.50)
    assert get_amount(account_id_2) == '2000.50'

    transaction_id_1 = create_transaction(account_id_2, account_id_1, 1000.50)
    assert get_amount(account_id_1) == '2001.00'
    assert get_amount(account_id_2) == '1000.00'

    print("*** test_transaction_ok: finished")
    return


def test_account_conflict():
    global session
    print("*** test_account_conflict: started")

    # Test 3 sequential create requests with the same idempotency token
    token = get_next_token()
    test_id = ""
    for _ in range(3):
        response = _create_account(token, 1000)
        assert response.status_code == 200
        account_id = response.content.decode("utf-8")
        print("account created", account_id)
        if test_id == "":
            test_id = account_id
        else:
            assert test_id == account_id
        assert account_id.startswith('A')
        assert len(account_id) >= 5
        print("account created", account_id)

    # this request should fail, amount differs for the token
    response = _create_account(token, 2000)
    assert response.status_code == 400
    print("account was not created as expected")

    print("*** test_account_conflict: finished")
    return


def test_transaction_conflict():
    print("*** test_transaction_conflict: started")
    account_id_1 = create_account(3000.50)
    assert get_amount(account_id_1) == '3000.50'

    account_id_2 = create_account(7000.50)
    assert get_amount(account_id_2) == '7000.50'

    # this request should fail, same accounts
    response = _create_transaction(get_next_token(), account_id_1, account_id_1, 1000.00)
    assert response.status_code == 400

    # Test 3 sequential create requests with the same idempotency token
    token = get_next_token()
    test_id = ""
    for _ in range(3):
        response = _create_transaction(token, account_id_2, account_id_1, 1000.50)
        assert response.status_code == 200
        transaction_id = response.content.decode("utf-8")
        print("transaction created", transaction_id)
        if test_id == "":
            test_id = transaction_id
        else:
            assert test_id == transaction_id
        assert get_amount(account_id_1) == '4001.00'
        assert get_amount(account_id_2) == '6000.00'

    # this request should fail, amount differs for the token
    response = _create_transaction(token, account_id_2, account_id_1, 1000.00)
    assert response.status_code == 400

    # this request should fail, accounts differ
    response = _create_transaction(token, account_id_1, account_id_2, 1000.50)
    assert response.status_code == 400

    print("*** test_transaction_conflict: finished")
    return


def test_multi_client_ok():
    print("*** test_multi_client_ok: started")

    # large numbers
    amount_1 = '123456789012345678901234567890.51'
    amount_2 = '123456789012345678901234567890.52'
    # same token for different sessions
    token = get_next_token()

    session_1 = create_session()
    assert amount_1 != amount_2
    account_id_1 = create_account(amount_1, token, session_id=session_1)
    test_amount = get_amount(account_id_1)
    assert test_amount == amount_1
    assert test_amount != amount_2

    session_2 = create_session()
    account_id_2 = create_account(amount_2, token, session_id=session_2)
    test_amount = get_amount(account_id_2)
    assert test_amount == amount_2
    assert test_amount != amount_1

    print("*** test_multi_client_ok: finished")
    return


if __name__ == "__main__":
    client_id = create_session()
    test_transaction_ok()
    test_account_conflict()
    test_transaction_conflict()
    test_multi_client_ok()
