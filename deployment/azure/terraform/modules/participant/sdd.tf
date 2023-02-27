resource "local_file" "sdd" {
  content  = <<EOT
    {
        "selfDescriptionCredential": {
            "@context": [
                "http://www.w3.org/ns/shacl#",
                "http://www.w3.org/2001/XMLSchema#",
                "http://w3id.org/gaia-x/participant#",
                "@nest"
            ],
            "@id": "https://compliance.gaia-x.eu/.well-known/participant.json",
            "@type": [
                "VerifiableCredential",
                "LegalPerson"
            ],
            "credentialSubject": {
                "id": "did:compliance.gaia-x.eu",
                "gx-participant:registrationNumber": {
                    "@type": "xsd:string",
                    "@value": "${var.participant_name}"
                },
                "gx-participant:headquarterAddress": {
                    "@type": "gx-participant:Address",
                    "gx-participant:country": {
                        "@value": "${var.participant_country}",
                        "@type": "xsd:string"
                    }
                },
                "gx-participant:legalAddress": {
                    "@type": "gx-participant:Address",
                    "gx-participant:country": {
                        "@value": "${var.participant_country}",
                        "@type": "xsd:string"
                    }
                }
            },
            "proof": {
                "type": "JsonWebKey2020",
                "created": "2022-07-05T14:43:06.543Z",
                "proofPurpose": "assertionMethod",
                "verificationMethod": "did:web:test.delta-dao.com",
                "jws": "eyJhbGciOiJQUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..WkJ7XuHlg2zQxoyFyAkt-QGzMdeCQRhylNbtu8CClGx11B49Z_zKm-HAEZv-NLupapvVYswL2JjoCcEQPhUEqhYruFIXcDwSTkBRpIxo084fytVMZtM2HDHV2snYpn7zUpfVzCOb-T2pkWkbmVvAOcSOg9OLPPWO1ypqUcimaEgdkyEHK-HFAuuqtll7K_5xP0-4_anXbF7Rr4aj0WQ5_glJMD8C2wjGir5DZB_vCOygVuprUL0OSPjdxB-4k6F1UPGr8MJ-IClfXpRaV0zdjkCZseCm4dIi9SOKGYTK609atCbhG3iQdukuZLhYJ8XhHyYv_5vGjkIVeayES78R1Q"
            }
        },
        "complianceCredential": {
            "@context": [
                "https://www.w3.org/2018/credentials/v1"
            ],
            "@type": [
                "VerifiableCredential",
                "ParticipantCredential"
            ],
            "id": "https://catalogue.gaia-x.eu/credentials/ParticipantCredential/1657032187885",
            "issuer": "did:web:compliance.gaia-x.eu",
            "issuanceDate": "2022-07-05T14:43:07.885Z",
            "credentialSubject": {
                "id": "did:compliance.gaia-x.eu",
                "hash": "bd3a7c2819c80b2a4ccf24151ea2212aeffd5aecafce2a4f9672b7f707ed76a3"
            },
            "proof": {
                "type": "JsonWebKey2020",
                "created": "2022-07-05T14:43:07.885Z",
                "proofPurpose": "assertionMethod",
                "jws": "eyJhbGciOiJQUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..Sfbi2OjSoS4MLJA_ZHbAxjeWp5rD9t652mo7tV-zEV2sJjOYGOEGS7of9P8BDyHb1QJ1tNScJQu83aIEEN-NiYZGpWHfHQ39n0TnZHRiUI0GkbX8W2XDaL2wDIa62Q30v_-PdcnOruApcOIyIBVVFfel9b8OZU3L0lb0z71AO17kgDYWVMauchn9DFQrPcbPycn39dzwwoh2ojnIn6HZ5JtIeBsjzeLq2EnzNgkSjXiubHZRPjjPwM9ZqMl_Bmo0Nta18Kk8r3j5X0974xvbV63f7dfbHglNBnvc4ncEnWiRqIaF1MoMsw_EhUrVETrfrxju4Bm9cFunOIeKf8FuUQ",
                "verificationMethod": "did:web:compliance.gaia-x.eu"
            }
        }
    }
  EOT
  filename = "${path.module}/resources/self-description/${var.participant_name}/sdd.json"
}