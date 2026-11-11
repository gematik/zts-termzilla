<img align="right" width="250" height="47" src="https://raw.githubusercontent.com/gematik/gematik.github.io/master/Gematik_Logo_Flag_With_Background.png"/> <br/> 

<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
       <ul>
        <li><a href="#release-notes">Release Notes</a></li>
        <li><a href="#contributions-and-acknowledgements">Contributions and Acknowledgements</a></li>
      </ul>
	</li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#loinc-snomed">How to use LOINC and SNOMED</a></li>
        <li><a href="#documentation">Documentation</a></li>
      </ul>
    </li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#additional-notes">Additional Notes and Disclaimer from gematik GmbH</a></li>
    <li><a href="#contact">Contact</a></li>
  </ol>
</details>

## About The Project

A Terminology FHIR Server implementation, which includes support for SNOMED-CT, Loinc and other code sytems. 

**Please note:**
This project is provided as an experimental release for the joint community hackathon involving Germany, Austria and Switzerland.
It is under active development and is not yet feature-complete or production-ready. Features, APIs and service behavior may change without prior notice.
This release is intended solely for exploration and experimentation. It should not be used in production environments.

### Release Notes

See [ReleaseNotes.md](./ReleaseNotes.md) for all information regarding the (newest) releases.

### Contributions and Acknowledgements

This open source project was developed in cooperation with the German Federal Institute for Drugs and Medical Devices (BfArM) on the basis of Section 355 (12-14) of the German Social Code Book V (SGB V).
As part of the projects implementation, the fbeta GmbH and Fraunhofer FOKUS were commissioned to provide software development services.

We would like to thank all parties involved for their constructive and trusted collaboration.

## Getting started

You can start termzilla with the following command:
```
docker compose -f compose-test.yaml up
```

and clean up with
```
docker compose -f compose-test.yaml down
```

### How to use LOINC and SNOMED

If you want to use LOINC or SNOMED with termzilla, you have to build your own data containers.
You can use the Dockerfile example below:

```Dockerfile
FROM busybox:version

CMD [ "echo", "ZTS Terminologies Resources" ]

WORKDIR /data

# Default USERID and GROUPID
ARG USERID=10000
ARG GROUPID=10000

# Run as User (not root)
USER $USERID:$GROUPID

# Copy all files
COPY --chown=$USERID:$GROUPID . /data
```

For SNOMED you can get the data [here](https://mlds.ihtsdotools.org/#/landing/DE?lang=de) and for LOINC [here](https://loinc.org/downloads).

Define your custom `Dockerfile` in a folder of your choice, put next to it the downloaded ZIP for LOINC or for SNOMED and issue the following commands:

```sh
cd path/where/Dockerfile/is/defined
# Build Loinc
docker build -t loinc:latest .
# Build Snomed
docker build t snomed:latest
```

Just uncomment the lines in compose-test.yaml, replace the `snomed-content.image` or `loinc-content.image` with your own built images when you're ready. 

### Documentation

See [this link](./documentation/README.md) for more documentation details.

## Contributing
If you want to contribute, please check our [CONTRIBUTING.md](./CONTRIBUTING.md).

## License

Copyright 2026 gematik GmbH

Apache License, Version 2.0

See the [LICENSE](./LICENSE) for the specific language governing permissions and limitations under the License

## Additional Notes and Disclaimer from gematik GmbH

1. Copyright notice: Each published work result is accompanied by an explicit statement of the license conditions for use. These are regularly typical conditions in connection with open source or free software. Programs described/provided/linked here are free software, unless otherwise stated.
2. Permission notice: Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
1. The copyright notice (Item 1) and the permission notice (Item 2) shall be included in all copies or substantial portions of the Software.
2. The software is provided "as is" without warranty of any kind, either express or implied, including, but not limited to, the warranties of fitness for a particular purpose, merchantability, and/or non-infringement. The authors or copyright holders shall not be liable in any manner whatsoever for any damages or other claims arising from, out of or in connection with the software or the use or other dealings with the software, whether in an action of contract, tort, or otherwise.
3. We take open source license compliance very seriously. We are always striving to achieve compliance at all times and to improve our processes. If you find any issues or have any suggestions or comments, or if you see any other ways in which we can improve, please reach out to: ospo@gematik.de
3. Parts of this software and - in isolated cases - content such as text or images may have been developed using the support of AI tools. They are subject to the same reviews, tests, and security checks as any other contribution. The functionality of the software itself is not based on AI decisions.

## Contact
We take open source license compliance very seriously. We are always striving to achieve compliance at all times and to improve our processes.
This software is currently being tested to ensure its technical quality and legal compliance. Your feedback is highly valued.
If you find any issues or have any suggestions or comments, or if you see any other ways in which we can improve, please reach out to: zts@gematik.de.