package com.gmail.mrphpfan;

public class Bal implements Comparable {
	private final String name;
	private final Double balance;

	public Bal(String name, double balance) {
		this.name = name;
		this.balance = balance;
	}

	public String getName() {
		return name;
	}

	public Double getBalance() {
		return balance;
	}

	@Override
	public int compareTo(Object o) {
		if(o instanceof Bal) {
			Bal other = (Bal) o;
			if(this.balance > other.balance) {
				return -1;
			}
			else if(this.balance < other.balance) {
				return 1;
			}
			else {
				return 0;
			}
		}
		else {
			return 0;
		}
	}

	public String toString() {
		return name + ": " + balance;
	}
}
