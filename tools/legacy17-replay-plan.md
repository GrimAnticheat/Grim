# Legacy17 Replay Plan

## Goal
Build a repeatable manual or scripted replay checklist for `legacy17` so detection tuning does not rely on feel.

## Core Scenarios
1. Normal movement
   - walking
   - sprinting
   - jumping
   - edge movement on slabs, stairs, fences, carpets
2. Combat geometry
   - legit close-range hits
   - sprint reset hits
   - knockback trade hits
   - teleport or pearl shortly before combat
3. Scaffold and placement
   - normal bridge
   - diagonal bridge
   - same-tick spam placement
   - packet cursor edge cases
4. Break checks
   - normal mining
   - long-distance mining attempts
   - off-angle mining
   - rapid multi-block break attempts
5. Lag compensation
   - high RTT
   - jitter spikes
   - server velocity before movement packet burst
   - delayed chunk/block updates

## What To Record
- player name
- check name
- source path (`PACKET_*`, `BUKKIT_MOVE_EVENT`, fallback)
- RTT and jitter
- pending world changes count
- prediction best profile
- compensated block at feet / below / target block
- whether fallback path was used

## Pass Criteria
- no false flags in normal movement and legit combat scenarios
- scaffold checks only trigger on clearly invalid cursor/rotation patterns
- break checks do not trigger on legit close-range mining
- packet-first path handles most detections without legacy fallback

## Regression Hotspots
- `Prediction`
- `Phase`
- `GroundSpoof`
- `NoSlow`
- `Reach`
- `RotationBreak`
- `FarBreak`
- `FabricatedPlace`

## Recommended Order
1. packet/world cache validation
2. prediction + phase validation
3. scaffold validation
4. break validation
5. combat heuristic validation
