#!/usr/bin/env python3
"""Receive integration-mod messages through a real offline-mode Minecraft connection."""
import argparse
import json
import re
import socket
import struct
import time
import uuid
import hashlib
import zlib

parser = argparse.ArgumentParser()
parser.add_argument('--port', type=int, required=True)
parser.add_argument('--log', required=True)
parser.add_argument('--expected', type=int, required=True)
parser.add_argument('--verbose', action='store_true')
parser.add_argument('--copies', type=int, default=2)
parser.add_argument('--legacy', type=int, default=1)
parser.add_argument('--bukkit-rcon-port', type=int)
args = parser.parse_args()
log = open(args.log).read()
ids = json.loads(re.findall(r'GRIM_MESSAGE_TEST IDS=(.*)', log)[-1])

def varint(value):
    value &= 0xffffffff
    result = bytearray()
    while value > 127:
        result.append((value & 127) | 128)
        value >>= 7
    result.append(value)
    return bytes(result)

def string(value):
    encoded = value.encode('utf-8')
    return varint(len(encoded)) + encoded

def read_varint(read):
    value = 0
    for shift in range(0, 35, 7):
        byte = read(1)[0]
        value |= (byte & 127) << shift
        if not byte & 128:
            return value
    raise ValueError('Invalid VarInt')

sock = socket.create_connection(('127.0.0.1', args.port), timeout=20)
sock.settimeout(20)
compression = None

def read(size):
    result = bytearray()
    while len(result) < size:
        chunk = sock.recv(size - len(result))
        if not chunk:
            raise EOFError('Server closed the connection')
        result.extend(chunk)
    return bytes(result)

def send(packet_id, payload=b''):
    payload = varint(packet_id) + payload
    if compression is not None:
        payload = (varint(len(payload)) + zlib.compress(payload)) if len(payload) >= compression else b'\0' + payload
    sock.sendall(varint(len(payload)) + payload)

def receive():
    import io
    payload = read(read_varint(read))
    if compression is not None:
        stream = io.BytesIO(payload)
        size = read_varint(stream.read)
        payload = zlib.decompress(stream.read()) if size else stream.read()
    stream = io.BytesIO(payload)
    packet_id = read_varint(stream.read)
    return packet_id, stream.read()

send(0, varint(ids['protocol']) + string('localhost') + struct.pack('>H', args.port) + varint(2))
send(0, string('MessageTest') + uuid.UUID(bytes=hashlib.md5(b'OfflinePlayer:MessageTest').digest(), version=3).bytes)
phase = 'login'
received = []
commands_sent = False

def rcon(command):
    connection = socket.create_connection(('127.0.0.1', args.bukkit_rcon_port), timeout=5)
    def request(kind, text):
        body = struct.pack('<ii', 1, kind) + text.encode() + b'\0\0'
        connection.sendall(struct.pack('<i', len(body)) + body)
        size = struct.unpack('<i', connection.recv(4))[0]
        result = b''
        while len(result) < size:
            result += connection.recv(size - len(result))
        return result
    request(3, 'grim-message-e2e')
    result = request(2, command)
    connection.close()
    return result

def bukkit_messages():
    rcon('op MessageTest')
    result = rcon('execute as MessageTest run grim alerts')
    if b'disabled' in result.lower():
        rcon('execute as MessageTest run grim alerts')
    samples = {
        'plain': 'Hello', 'rgb': '<#12abef><bold>RGB',
        'hover': "<hover:show_text:'Hover text'>Hover</hover>",
        'click': "<click:run_command:'/grim alerts'>Click</click>",
        'item': "<hover:show_item:'minecraft:stone':2>Stone</hover>",
        'entity': "<hover:show_entity:'minecraft:pig':'00000000-0000-0001-0000-000000000002':'Pig'>Pig</hover>",
        'translation': "<lang:chat.type.text:'name':'message'>", 'unicode': 'Hello🌍漢字',
    }
    for name, text in samples.items():
        rcon('grim sendalert GRIM_MESSAGE_TEST:' + name + ':' + text)

started = time.monotonic()
while len(received) < args.expected and time.monotonic() - started < 60:
    packet_id, payload = receive()
    if args.verbose:
        print(phase, hex(packet_id), len(payload), flush=True)
    if phase == 'login':
        if packet_id == 3:
            import io
            compression = read_varint(io.BytesIO(payload).read)
        elif packet_id == 2:
            send(3)
            phase = 'configuration'
            send(0, string('en_us') + b'\x02' + varint(0) + b'\x01\x7f' + varint(1) + b'\x00\x01' + varint(0))
        elif packet_id == 0:
            raise RuntimeError('Login rejected: ' + repr(payload))
        elif packet_id == 4:
            import io
            transaction = read_varint(io.BytesIO(payload).read)
            send(2, varint(transaction) + b'\0')
    elif phase == 'configuration':
        if packet_id == 5:
            send(5, payload)  # common ping/pong barrier
        elif packet_id == 4:
            send(4, payload)
        elif packet_id == ids['known_packs']:
            send(ids['known_packs_reply'], varint(0))
        elif packet_id == ids['config_end']:
            send(ids['config_end_ack'])
            phase = 'play'
            if ids['loaded'] >= 0:
                send(ids['loaded'])
    else:
        if args.bukkit_rcon_port and not commands_sent:
            commands_sent = True
            bukkit_messages()
        if packet_id == ids['keepalive']:
            send(ids['keepalive_reply'], payload)
        elif packet_id == ids['system_chat'] and b'GRIM_MESSAGE_TEST:' in payload:
            match = re.search(rb'GRIM_MESSAGE_TEST:([a-z-]+):', payload)
            if match:
                if match.group(1) == b'legacy':
                    assert '§'.encode() not in payload, 'Legacy formatting was sent as literal text'
                received.append(match.group(1).decode())
sock.close()
from collections import Counter
counts = Counter(received)
assert len(received) == args.expected, (len(received), args.expected, counts)
assert counts.pop('legacy', 0) == args.legacy, counts
assert all(count == args.copies for count in counts.values()), counts
print(json.dumps({'received': len(received), 'compression': compression, 'fixtures': dict(counts)}, sort_keys=True))
