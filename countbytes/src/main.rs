use std::fs::File;
use std::io::prelude::*;
use std::io::BufReader;
use std::env;

fn main() {
    let mut count = vec![0u32; 256];
    println!("{}", count.len());

    let file = File::open(env::args().nth(1).unwrap()).unwrap();
    let file = BufReader::new(file);

    for byte in file.bytes() {
        count[byte.unwrap() as usize] += 1;
    }

    for (i, count) in count.iter().enumerate() {
        println!("{}: {}", (((i as u8) as i8)), count);
    }


}
