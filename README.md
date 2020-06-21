# mobile-node

PoC Freenet mobile node

## Goals

- Be able to start and stop a node
- See logs and other basic information

## Non-goals

- Content browser or upload
- Install plugins
- Change node configuration
- Detect wifi/data, on battery/on power

## How

By adding freenet.jar, wrapper.jar and other jars as dependences to gradle:


    dependencies {
        ...
        implementation files('libs/freenet.jar')
        ...   
    }   

Once added as libraries we can interact with Freenet:


    public class MainActivity extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {

            freenet.node.NodeStarter.main(args);

        }    


Additionally we found that some changes would be required to interact with the filesystem. Some cryprographic function need some work to make it work as well.

All this work is being done at [mobile-node](https://github.com/desyncr/fred/tree/mobile-node) branch and slightly documented [here](https://github.com/desyncr/fred/blob/mobile-node/building-fred.md).

A log of the node (trying to) running can be seen [here](https://gist.github.com/desyncr/3c2f0316495732b03f367ed47daad03b).

### Limitations

- Node won't be able to self update (in theory)

## Building

    git clone https://github.com/desyncr/fred
    cd fred
    git checkout mobile-node
    ./gradlew jar
     > copy build/libs/freenet.jar
