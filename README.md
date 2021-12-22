# mikrotik-list-importer
Mikrotik List Import is a tool to import any IP blocklists (for example the lists from https://iplists.firehol.org) into an address list in your mikrotik router.

## Example application.conf

```
mikrotik.host = "10.2.1.1"
mikrotik.user = "api"
mikrotik.password = ""

lists = [
  {
    name: "threats",
    sources: [
      "https://www.spamhaus.org/drop/drop.txt",
      "https://www.spamhaus.org/drop/edrop.txt",
      "https://iplists.firehol.org/files/et_block.netset",
      "https://iplists.firehol.org/files/dshield_30d.netset",
      "https://iplists.firehol.org/files/et_tor.ipset",
      "https://iplists.firehol.org/files/et_compromised.ipset",
      "http://cinsscore.com/list/ci-badguys.txt"
    ],
    update-interval: "1h"
  }
]
```

## Example rules
```
chain=prerouting action=drop log=yes log-prefix="THREATS_IN" src-address-list=threats
chain=prerouting action=drop log=yes log-prefix="THREATS_OUT" dst-address-list=threats
```
