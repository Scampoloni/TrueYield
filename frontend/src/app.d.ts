// See https://svelte.dev/docs/kit/types#app.d.ts

declare global {
  namespace App {
    // interface Error {}
    interface Locals {
      isAuthenticated: boolean;
      user: {
        sub?: string;
        name?: string;
        email?: string;
        nickname?: string;
        picture?: string;
        user_roles?: string[];
      } | null;
      jwt_token: string | null;
    }
    // interface PageData {}
    // interface PageState {}
    // interface Platform {}
  }
}

export {};
