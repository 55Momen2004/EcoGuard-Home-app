# EcoGuard Home Bluetooth Command Protocol

## Phone to Arduino commands

| Command | Action |
|---|---|
| `A` | Start smart automation mode |
| `H` | Stop smart automation mode and turn off CH5, CH6, CH7 automation loads |
| `L` | Turn CH1 ON |
| `K` | Turn CH1 OFF |
| `R` | Turn CH2 ON and move servo to 80 degrees |
| `T` | Turn CH2 OFF and move servo to 180 degrees |
| `Q` | Turn CH3 ON |
| `W` | Turn CH3 OFF |
| `G` | Turn CH4 ON |
| `F` | Turn CH4 OFF |
| `D` | Turn CH5 ON |
| `V` | Turn CH5 OFF |
| `E` | Turn CH6 ON |
| `N` | Turn CH6 OFF |
| `C` | Turn CH7 ON |
| `M` | Turn CH7 OFF |
| `I` | Turn CH8 ON |
| `O` | Turn CH8 OFF |
| `S` | Request live status packet |
| `X` | Silence buzzer alarm |

## Arduino to phone messages

### Status packet

```text
STATUS;GAS=0;FLAME=0;ALARM=0;SMART=0;GAS_VALUE=245;FLAME_VALUE=720;RELAYS=00000000
```

### Alert packet

```text
ALERT;TYPE=FIRE;GAS=0;FLAME=1;ALARM=1;GAS_VALUE=245;FLAME_VALUE=120;RELAYS=00000000
```

`TYPE` can be `FIRE`, `GAS`, or `BOTH`.

### Acknowledgement packet

```text
ACK;CMD=R;RELAYS=01000000
```

## Relay status format

`RELAYS=01000000` means CH2 is ON and all other channels are OFF. The system uses active-low relay logic: Arduino pin LOW means relay ON, and HIGH means relay OFF.
